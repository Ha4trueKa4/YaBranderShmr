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

        init(
            id: UUID,
            text: String,
            createdAt: Date,
            modelID: String,
            deliveryState: DeliveryState = .pending
        ) {
            self.id = id
            self.text = text
            self.createdAt = createdAt
            self.modelID = modelID
            self.deliveryState = deliveryState
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
        }
    }

    struct CachedChat: Codable {
        let messages: [RemoteChatMessage]
        let isPublic: Bool
        var pendingMessages: [PendingMessage]

        init(
            messages: [RemoteChatMessage],
            isPublic: Bool,
            pendingMessages: [PendingMessage] = []
        ) {
            self.messages = messages
            self.isPublic = isPublic
            self.pendingMessages = pendingMessages
        }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            messages = try container.decode([RemoteChatMessage].self, forKey: .messages)
            isPublic = try container.decode(Bool.self, forKey: .isPublic)
            pendingMessages = try container.decodeIfPresent(
                [PendingMessage].self,
                forKey: .pendingMessages
            ) ?? []
        }
    }

    private let directory: URL

    init() {
        directory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("chat-cache", isDirectory: true)
    }

    func load(projectID: UUID) -> CachedChat? {
        guard let data = try? Data(contentsOf: fileURL(for: projectID)) else { return nil }
        return try? JSONDecoder().decode(CachedChat.self, from: data)
    }

    func save(projectID: UUID, messages: [RemoteChatMessage], isPublic: Bool) {
        let existing = load(projectID: projectID)
        let cache = CachedChat(
            messages: messages,
            isPublic: isPublic,
            pendingMessages: existing?.pendingMessages ?? []
        )
        write(cache, projectID: projectID)
    }

    func savePendingMessages(_ pendingMessages: [PendingMessage], for projectID: UUID) {
        var cache = load(projectID: projectID) ?? CachedChat(messages: [], isPublic: false)
        cache.pendingMessages = pendingMessages
        write(cache, projectID: projectID)
    }

    private func write(_ cache: CachedChat, projectID: UUID) {
        guard let data = try? JSONEncoder().encode(cache) else { return }

        do {
            try FileManager.default.createDirectory(
                at: directory,
                withIntermediateDirectories: true
            )
            try data.write(to: fileURL(for: projectID), options: .atomic)
        } catch {
            // Caching is best-effort; the chat still works online.
        }
    }

    private func fileURL(for projectID: UUID) -> URL {
        directory.appendingPathComponent("chat-\(projectID.uuidString.lowercased())").appendingPathExtension("json")
    }
}
