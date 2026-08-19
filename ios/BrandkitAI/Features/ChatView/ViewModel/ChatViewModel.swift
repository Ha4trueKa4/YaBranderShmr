import Foundation

/// Холдер для токена обсёрвера `NotificationCenter`. Вынесен из
/// `@MainActor`-класса и помечен `nonisolated`, чтобы `deinit` (nonisolated)
/// мог снять обсёрвер без нарушений изоляции. Запись в `init` (MainActor) и
/// чтение в `deinit` не пересекаются по времени.
private nonisolated final class ChatObserverHolder: @unchecked Sendable {
    var token: NSObjectProtocol?
}

@MainActor
@Observable
final class ChatViewModel {
    enum ProjectPrivacy: String, CaseIterable, Identifiable {
        case privateProject
        case publicShowcase

        var id: Self { self }

        var title: String {
            switch self {
            case .privateProject: "Private Project"
            case .publicShowcase: "Public Showcase"
            }
        }

        var shortTitle: String {
            switch self {
            case .privateProject: "Private"
            case .publicShowcase: "Public"
            }
        }

        var subtitle: String {
            switch self {
            case .privateProject: "Only workspace members can edit and view"
            case .publicShowcase: "Index project in public showcase database"
            }
        }

        var icon: String {
            switch self {
            case .privateProject: "lock"
            case .publicShowcase: "globe"
            }
        }

        init(isPublic: Bool) {
            self = isPublic ? .publicShowcase : .privateProject
        }
    }

    struct MessageSection: Identifiable {
        let date: Date
        let messages: [ChatMessage]

        var id: Date { date }
    }

    let projectID: UUID

    private(set) var messages: [ChatMessage] = []
    private(set) var sections: [MessageSection] = []
    private(set) var lastMessageID: ChatMessage.ID?
    private(set) var isLoading = false
    private(set) var isGenerating = false
    private(set) var isOffline = false
    private(set) var loadFailed = false
    private(set) var pendingCount = 0
    var privacy = ProjectPrivacy.privateProject

    private let service: any ProjectServiceProtocol
    private var didLoad = false
    private var versionURLs: [UUID: URL] = [:]
    private var pendingMessages: [ChatCache.PendingMessage] = []
    private let observerHolder = ChatObserverHolder()

    init(
        projectID: UUID,
        service: any ProjectServiceProtocol = ProjectService()
    ) {
        self.projectID = projectID
        self.service = service
        observerHolder.token = NotificationCenter.default.addObserver(
            forName: .networkDidBecomeAvailable,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            Task { @MainActor in
                await self?.flushPendingMessages()
            }
        }
    }

    deinit {
        if let token = observerHolder.token {
            NotificationCenter.default.removeObserver(token)
        }
    }


    func load() async {
        guard !didLoad, !isLoading else { return }
        if let cached = await ChatCache.shared.load(projectID: projectID) {
            replaceMessages(cached.messages)
            privacy = ProjectPrivacy(isPublic: cached.isPublic)
            restorePendingMessages(cached.pendingMessages)
        }
        await reload()
    }

    func retry() async {
        guard !isLoading else { return }
        didLoad = false
        await reload()
    }

    func sendMessage(_ text: String) async {
        let text = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, !isGenerating else { return }

        appendMessage(ChatMessage(type: .user, text: text, date: Date()))
        isGenerating = true

        do {
            let generatedURL = try await service.generate(projectID: projectID, prompt: text)
            try? await refreshChatAfterGenerate(generatedURL: generatedURL)
            isGenerating = false
            await flushPendingMessages()
        } catch {
            isGenerating = false
            markLastMessagePending()
            await queuePendingMessage(text)
            await flushPendingMessages()
        }
    }

    func flushPendingMessages() async {
        guard !isGenerating, !pendingMessages.isEmpty else { return }
        isGenerating = true
        defer { isGenerating = false }

        while !pendingMessages.isEmpty {
            let prompt = pendingMessages[0]
            do {
                let generatedURL = try await service.generate(projectID: projectID, prompt: prompt.text)
                pendingMessages.removeFirst()
                try await refreshChatAfterGenerate(generatedURL: generatedURL)
            } catch {
                break
            }
        }

        pendingCount = pendingMessages.count
        await ChatCache.shared.savePendingMessages(pendingMessages, for: projectID)
    }

    func updatePrivacy(_ newPrivacy: ProjectPrivacy) async {
        guard newPrivacy != privacy else { return }
        let previousPrivacy = privacy
        privacy = newPrivacy
        do {
            let project = try await service.updateVisibility(
                projectID: projectID,
                isPublic: newPrivacy == .publicShowcase
            )
            privacy = ProjectPrivacy(isPublic: project.isPublic)
        } catch {
            privacy = previousPrivacy
        }
    }

    func dateTitle(for date: Date) -> String {
        let calendar = Calendar.current

        if calendar.isDateInToday(date) { return "Today" }
        if calendar.isDateInYesterday(date) { return "Yesterday" }
        return date.formatted(.dateTime.month(.abbreviated).day().year())
    }

    func timeTitle(for date: Date) -> String {
        date.formatted(date: .omitted, time: .shortened)
    }

    func versionURL(for versionID: UUID) async throws -> URL {
        if let url = versionURLs[versionID], !URLSignature.isExpired(url) {
            return url
        }
        let url = try await service.fetchVersionURL(projectID: projectID, versionID: versionID)
        versionURLs[versionID] = url
        return url
    }

    /// Read-шаг после успешной генерации: подтягивает свежую историю чата,
    /// маппит URL новой версии, разогревает кеш SVG и сохраняет состояние.
    /// Ошибки `fetchChat` здесь некритичны — генерация уже принята бэкендом,
    /// поэтому вызывающий снимает промпт с очереди до этого вызова.
    private func refreshChatAfterGenerate(generatedURL: URL?) async throws {
        let remoteMessages = try await service.fetchChat(projectID: projectID)

        if let generatedURL,
           let versionID = remoteMessages.reversed().compactMap(\.versionID).first {
            versionURLs[versionID] = generatedURL
            let cacheKey = SVGCache.versionKey(projectID: projectID, versionID: versionID)
            _ = try? await SVGCache.shared.data(for: generatedURL, key: cacheKey)
        }

        applyRemoteMessages(remoteMessages)
        isOffline = false
        await ChatCache.shared.save(
            projectID: projectID,
            messages: remoteMessages,
            isPublic: privacy == .publicShowcase
        )
    }

    private func restorePendingMessages(_ messages: [ChatCache.PendingMessage]) {
        pendingMessages = messages
        pendingCount = messages.count
        for pending in messages {
            appendMessage(ChatMessage(
                type: .user,
                text: pending.text,
                date: pending.createdAt,
                isPending: true
            ))
        }
    }

    private func markLastMessagePending() {
        guard let lastID = lastMessageID,
              let index = messages.firstIndex(where: { $0.id == lastID }) else { return }
        messages[index].isPending = true
    }

    private func queuePendingMessage(_ text: String) async {
        pendingMessages.append(ChatCache.PendingMessage(text: text, createdAt: Date()))
        pendingCount = pendingMessages.count
        await ChatCache.shared.savePendingMessages(pendingMessages, for: projectID)
    }

    private func reload() async {
        isLoading = true
        isOffline = false
        loadFailed = false

        do {
            async let project = service.fetchProject(id: projectID)
            async let remoteMessages = service.fetchChat(projectID: projectID)
            let (fetchedProject, fetchedMessages) = try await (project, remoteMessages)
            privacy = ProjectPrivacy(isPublic: fetchedProject.isPublic)
            applyRemoteMessages(fetchedMessages)
            didLoad = true
            await ChatCache.shared.save(
                projectID: projectID,
                messages: fetchedMessages,
                isPublic: fetchedProject.isPublic
            )
        } catch {
            if messages.isEmpty {
                loadFailed = true
            } else {
                isOffline = true
            }
        }
        isLoading = false

        if !pendingMessages.isEmpty {
            await flushPendingMessages()
        }
    }

    private func applyRemoteMessages(_ remoteMessages: [RemoteChatMessage]) {
        replaceMessages(remoteMessages)
        mergePendingMessages()
    }

    private func mergePendingMessages() {
        for pending in pendingMessages {
            let alreadyPresent = messages.contains { message in
                message.type == .user && message.text == pending.text
            }
            if !alreadyPresent {
                appendMessage(ChatMessage(
                    type: .user,
                    text: pending.text,
                    date: pending.createdAt,
                    isPending: true
                ))
            }
        }
    }

    private func replaceMessages(_ remoteMessages: [RemoteChatMessage]) {
        let mapped = remoteMessages.map { remoteMessage in
            ChatMessage(
                type: remoteMessage.role.lowercased() == "assistant" ? .assistant : .user,
                text: remoteMessage.role.lowercased() == "assistant"
                    ? "Generated a new version."
                    : (remoteMessage.text ?? ""),
                date: remoteMessage.createdAt,
                attachment: remoteMessage.versionID.map {
                    .version(id: $0, url: versionURLs[$0])
                } ?? .none
            )
        }
        messages = mapped
        sections = makeSections(from: mapped)
        lastMessageID = mapped.last?.id
    }

    private func appendMessage(_ message: ChatMessage) {
        messages.append(message)
        lastMessageID = message.id
        let calendar = Calendar.current
        let sectionDate = calendar.startOfDay(for: message.date)

        guard let sectionIndex = sections.firstIndex(where: { $0.date == sectionDate }) else {
            sections.append(MessageSection(date: sectionDate, messages: [message]))
            sections.sort { $0.date < $1.date }
            return
        }

        var sectionMessages = sections[sectionIndex].messages
        sectionMessages.append(message)
        sections[sectionIndex] = MessageSection(date: sectionDate, messages: sectionMessages)
    }

    private func makeSections(from messages: [ChatMessage]) -> [MessageSection] {
        let calendar = Calendar.current
        let groupedMessages = Dictionary(grouping: messages) {
            calendar.startOfDay(for: $0.date)
        }

        return groupedMessages.keys.sorted().map { date in
            MessageSection(
                date: date,
                messages: groupedMessages[date, default: []].sorted { $0.date < $1.date }
            )
        }
    }

    #if DEBUG
    static func makePreview(
        projectID: UUID = UUID(),
        service: any ProjectServiceProtocol = ProjectService()
    ) -> ChatViewModel {
        let viewModel = ChatViewModel(projectID: projectID, service: service)
        let now = Date()

        let messages: [RemoteChatMessage] = [
            RemoteChatMessage(
                role: "user",
                text: "Create a modern logo for my coffee brand",
                versionID: nil,
                createdAt: now.addingTimeInterval(-600)
            ),
            RemoteChatMessage(
                role: "assistant",
                text: "Generated a new version.",
                versionID: UUID(),
                createdAt: now.addingTimeInterval(-595)
            ),
            RemoteChatMessage(
                role: "user",
                text: "Make the colors warmer and add a leaf icon",
                versionID: nil,
                createdAt: now.addingTimeInterval(-300)
            ),
            RemoteChatMessage(
                role: "assistant",
                text: "Generated a new version.",
                versionID: UUID(),
                createdAt: now.addingTimeInterval(-295)
            )
        ]

        viewModel.replaceMessages(messages)
        viewModel.didLoad = true
        return viewModel
    }
    #endif
}
