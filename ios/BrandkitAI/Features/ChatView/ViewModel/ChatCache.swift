import Foundation

actor ChatCache {
    static let shared = ChatCache()

    struct PendingMessage: Codable, Equatable {
        enum DeliveryState: String, Codable {
            case pending
            case failed
        }

        let id: UUID
        let text: String
        let createdAt: Date
        let modelID: String
        var deliveryState: DeliveryState
        /// Момент попытки отправки с неизвестным исходом: соединение
        /// оборвалось после того, как запрос мог дойти до сервера. Перед
        /// повторной отправкой такая попытка сверяется с историей чата —
        /// повторный POST мог бы запустить вторую генерацию с тем же промптом.
        var ambiguousSince: Date?

        init(
            id: UUID,
            text: String,
            createdAt: Date,
            modelID: String,
            deliveryState: DeliveryState = .pending,
            ambiguousSince: Date? = nil
        ) {
            self.id = id
            self.text = text
            self.createdAt = createdAt
            self.modelID = modelID
            self.deliveryState = deliveryState
            self.ambiguousSince = ambiguousSince
        }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            id = try container.decodeIfPresent(UUID.self, forKey: .id) ?? UUID()
            text = try container.decode(String.self, forKey: .text)
            createdAt = try container.decode(Date.self, forKey: .createdAt)
            modelID = try container.decodeIfPresent(String.self, forKey: .modelID)
                ?? GenerationModel.defaultID
            deliveryState = try container.decodeIfPresent(
                DeliveryState.self,
                forKey: .deliveryState
            ) ?? .pending
            ambiguousSince = try container.decodeIfPresent(
                Date.self,
                forKey: .ambiguousSince
            )
        }
    }

    struct CachedChat: Codable {
        let messages: [RemoteChatMessage]
        let isPublic: Bool
        var pendingMessages: [PendingMessage]
        /// jobID генераций, чей статус сервер уже не отдаёт (404 — job живёт
        /// в памяти бэкенда и не переживает его перезапуск). Опрос таких job
        /// не возобновляется, чтобы каждое открытие чата не заканчивалось
        /// ошибкой «Generation status is unavailable».
        var lostJobIDs: Set<String>

        init(
            messages: [RemoteChatMessage],
            isPublic: Bool,
            pendingMessages: [PendingMessage] = [],
            lostJobIDs: Set<String> = []
        ) {
            self.messages = messages
            self.isPublic = isPublic
            self.pendingMessages = pendingMessages
            self.lostJobIDs = lostJobIDs
        }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            messages = try container.decode([RemoteChatMessage].self, forKey: .messages)
            isPublic = try container.decode(Bool.self, forKey: .isPublic)
            pendingMessages = try container.decodeIfPresent(
                [PendingMessage].self,
                forKey: .pendingMessages
            ) ?? []
            lostJobIDs = try container.decodeIfPresent(
                Set<String>.self,
                forKey: .lostJobIDs
            ) ?? []
        }
    }

    /// История чата: пере-получаемые с сервера данные, им место в
    /// очищаемом системой кеше.
    private struct HistoryFile: Codable {
        let messages: [RemoteChatMessage]
        let isPublic: Bool
    }

    /// Офлайн-очередь: неотправленный пользовательский текст и маркеры
    /// потерянных job. Это не кеш — iOS не должна иметь права молча
    /// удалять их под нехватку места, поэтому Application Support.
    private struct QueueFile: Codable {
        var pendingMessages: [PendingMessage]
        var lostJobIDs: Set<String>
    }

    /// Верхняя граница списка потерянных job: защиты от бессрочного роста
    /// (записи не имеют TTL — просто срезаем хвост).
    private static let lostJobIDsLimit = 50

    private let historyDirectory: URL
    private let queueDirectory: URL

    init() {
        historyDirectory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("chat-cache", isDirectory: true)
        queueDirectory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("chat-queue", isDirectory: true)
    }

    func load(projectID: UUID) -> CachedChat? {
        let history = loadHistory(projectID: projectID)
        let queue = loadQueue(projectID: projectID)
        if let history {
            return CachedChat(
                messages: history.messages,
                isPublic: history.isPublic,
                pendingMessages: queue.pendingMessages,
                lostJobIDs: queue.lostJobIDs
            )
        }
        // Истории нет (чат ни разу не грузился онлайн), но очередь существует
        // сама по себе — неотправленные сообщения не должны теряться.
        guard !queue.pendingMessages.isEmpty || !queue.lostJobIDs.isEmpty else { return nil }
        return CachedChat(
            messages: [],
            isPublic: false,
            pendingMessages: queue.pendingMessages,
            lostJobIDs: queue.lostJobIDs
        )
    }

    func save(projectID: UUID, messages: [RemoteChatMessage], isPublic: Bool) {
        write(
            HistoryFile(messages: messages, isPublic: isPublic),
            to: historyFileURL(for: projectID),
            directory: historyDirectory
        )
    }

    func savePendingMessages(_ pendingMessages: [PendingMessage], for projectID: UUID) {
        var queue = loadQueue(projectID: projectID)
        queue.pendingMessages = pendingMessages
        write(queue, to: queueFileURL(for: projectID), directory: queueDirectory)
    }

    func markJobLost(_ jobID: String, projectID: UUID) {
        var queue = loadQueue(projectID: projectID)
        guard !queue.lostJobIDs.contains(jobID) else { return }
        queue.lostJobIDs.insert(jobID)
        if queue.lostJobIDs.count > Self.lostJobIDsLimit {
            queue.lostJobIDs = Set(queue.lostJobIDs.suffix(Self.lostJobIDsLimit))
        }
        write(queue, to: queueFileURL(for: projectID), directory: queueDirectory)
    }

    private func loadHistory(projectID: UUID) -> HistoryFile? {
        guard let data = try? Data(contentsOf: historyFileURL(for: projectID)) else { return nil }
        if let file = try? JSONDecoder().decode(HistoryFile.self, from: data) {
            return file
        }
        // Легаси-файл до разделения хранилищ: история лежала в одном
        // Codable с очередью. Поля очереди здесь игнорируются.
        guard let legacy = try? JSONDecoder().decode(CachedChat.self, from: data) else { return nil }
        return HistoryFile(messages: legacy.messages, isPublic: legacy.isPublic)
    }

    /// Очередь живёт в Application Support. Если файла ещё нет (первый
    /// запуск после разделения хранилищ), переносит очередь из старого
    /// кеш-файла, где она лежала рядом с историей, и фиксирует её в новом
    /// месте — чтобы миграция не повторялась на каждом чтении.
    private func loadQueue(projectID: UUID) -> QueueFile {
        if let data = try? Data(contentsOf: queueFileURL(for: projectID)),
           let queue = try? JSONDecoder().decode(QueueFile.self, from: data) {
            return queue
        }
        let legacy = (try? Data(contentsOf: historyFileURL(for: projectID)))
            .flatMap { try? JSONDecoder().decode(CachedChat.self, from: $0) }
        let queue = QueueFile(
            pendingMessages: legacy?.pendingMessages ?? [],
            lostJobIDs: legacy?.lostJobIDs ?? []
        )
        write(queue, to: queueFileURL(for: projectID), directory: queueDirectory)
        return queue
    }

    private func write<T: Encodable>(_ value: T, to url: URL, directory: URL) {
        guard let data = try? JSONEncoder().encode(value) else { return }

        do {
            try FileManager.default.createDirectory(
                at: directory,
                withIntermediateDirectories: true
            )
            try data.write(to: url, options: .atomic)
        } catch {
            // Caching is best-effort; the chat still works online.
        }
    }

    private func historyFileURL(for projectID: UUID) -> URL {
        directoryURL(historyDirectory, projectID)
    }

    private func queueFileURL(for projectID: UUID) -> URL {
        directoryURL(queueDirectory, projectID)
    }

    private func directoryURL(_ directory: URL, _ projectID: UUID) -> URL {
        directory.appendingPathComponent("chat-\(projectID.uuidString.lowercased())")
            .appendingPathExtension("json")
    }
}
