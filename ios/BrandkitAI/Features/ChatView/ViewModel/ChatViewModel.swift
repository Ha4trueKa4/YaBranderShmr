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
    static let maxPromptLength = 5_000

    private static let selectedModelStorageKey = "chat.selectedGenerationModel"

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
    private(set) var isCancelling = false
    private(set) var isOffline = false
    private(set) var loadFailed = false
    private(set) var pendingCount = 0
    private(set) var sendErrorMessage: String?
    private(set) var availableModels: [GenerationModel] = []
    private(set) var isLoadingModels = false
    private(set) var modelsLoadFailed = false
    private(set) var selectedModelID: String
    private(set) var restorePrompt: String?
    private(set) var activeJobID: String?
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
        self.selectedModelID = UserDefaults.standard.string(
            forKey: Self.selectedModelStorageKey
        ) ?? GenerationModel.defaultID
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

    var selectedModel: GenerationModel {
        availableModels.first(where: { $0.id == selectedModelID })
            ?? GenerationModel(remoteID: selectedModelID, owner: nil)
    }

    func loadModels(force: Bool = false) async {
        guard !isLoadingModels else { return }
        guard force || availableModels.isEmpty else { return }

        isLoadingModels = true
        modelsLoadFailed = false
        defer { isLoadingModels = false }

        do {
            let models = try await service.fetchModels()
            guard !models.isEmpty else { throw NetworkError.invalidResponse }
            availableModels = models

            if !models.contains(where: { $0.id == selectedModelID }) {
                selectedModelID = models.first(where: { $0.id == GenerationModel.defaultID })?.id
                    ?? models[0].id
                persistSelectedModel()
            }
        } catch {
            modelsLoadFailed = true
        }
    }

    func selectModel(_ model: GenerationModel) {
        selectedModelID = model.id
        persistSelectedModel()
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
        guard text.count <= Self.maxPromptLength else {
            sendErrorMessage = "Message is too long (maximum 5000 characters)."
            return
        }

        sendErrorMessage = nil
        let chatMessage = ChatMessage(type: .user, text: text, date: Date())
        let modelID = selectedModel.id
        appendMessage(chatMessage)
        isGenerating = true

        let jobID: String
        do {
            jobID = try await service.generate(projectID: projectID, prompt: text)
        } catch NetworkError.generationInProgress(let existingJobID) {
            removeMessage(id: chatMessage.id)
            restorePrompt = text
            sendErrorMessage = "Another generation is in progress. Your message was not sent."
            activeJobID = existingJobID
            isCancelling = false
            await pollAndRefresh(jobID: existingJobID)
            isGenerating = false
            activeJobID = nil
            isCancelling = false
            guard !Task.isCancelled else { return }
            await flushPendingMessages()
            return
        } catch {
            isGenerating = false
            if shouldKeepPending(after: error) {
                markMessagePending(id: chatMessage.id)
                await queuePendingMessage(
                    id: chatMessage.id,
                    text: text,
                    createdAt: chatMessage.date,
                    modelID: modelID
                )
                sendErrorMessage = "Message is pending and will be sent when the connection recovers."
            } else {
                markMessageFailed(id: chatMessage.id)
                sendErrorMessage = failureMessage(for: error)
            }
            return
        }

        activeJobID = jobID
        isCancelling = false
        await pollAndRefresh(jobID: jobID)
        isGenerating = false
        activeJobID = nil
        isCancelling = false
        guard !Task.isCancelled else { return }
        await flushPendingMessages()
    }

    func flushPendingMessages() async {
        guard !isGenerating, !pendingMessages.isEmpty else { return }
        isGenerating = true
        defer { isGenerating = false }

        while !pendingMessages.isEmpty {
            if Task.isCancelled { break }
            let prompt = pendingMessages[0]
            guard prompt.text.count <= Self.maxPromptLength else {
                pendingMessages.removeFirst()
                pendingCount = pendingMessages.count
                markPendingMessageFailed(prompt)
                continue
            }

            let jobID: String
            do {
                jobID = try await service.generate(projectID: projectID, prompt: prompt.text)
            } catch NetworkError.generationInProgress(let existingJobID) {
                activeJobID = existingJobID
                isCancelling = false
                await pollAndRefresh(jobID: existingJobID)
                activeJobID = nil
                isCancelling = false
                break
            } catch {
                if shouldKeepPending(after: error) {
                    break
                }
                pendingMessages.removeFirst()
                pendingCount = pendingMessages.count
                markPendingMessageFailed(prompt)
                sendErrorMessage = failureMessage(for: error)
                continue
            }

            pendingMessages.removeFirst()
            pendingCount = pendingMessages.count
            sendErrorMessage = nil
            activeJobID = jobID
            isCancelling = false
            await pollAndRefresh(jobID: jobID)
            activeJobID = nil
            isCancelling = false
            markPendingMessageSent(prompt)
        }

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

    func clearSendError() {
        sendErrorMessage = nil
    }

    func consumeRestorePrompt() {
        restorePrompt = nil
    }

    func cancelGeneration() async {
        guard let jobID = activeJobID, !isCancelling else { return }
        isCancelling = true
        do {
            _ = try await service.cancelGeneration(projectID: projectID, jobID: jobID)
        } catch {
            isCancelling = false
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

    // MARK: - Long polling

    private enum GenerationOutcome {
        case done(url: URL)
        case error(message: String)
        case cancelled
    }

    private static let maxPollRetries = 10
    private static let pollMinDelayNs: UInt64 = 1_000_000_000        // 1 sec
    private static let pollMaxDelayNs: UInt64 = 15 * 1_000_000_000   // 15 sec

    /// Опрашивает статус фоновой задачи генерации через long-polling.
    /// При `pending`/`cancelling` немедленно открывает новое соединение.
    /// При разрыве делает retry с backoff, при оффлайне ждёт восстановления.
    /// После ~10 неудачных попыток или 404 (задача потеряна) выбрасывает ошибку.
    private func pollGeneration(jobID: String) async throws -> GenerationOutcome {
        var attempt = 0
        var delayNs = Self.pollMinDelayNs

        while true {
            try Task.checkCancellation()

            do {
                let status = try await service.generateStatus(projectID: projectID, jobID: jobID)
                switch status {
                case .pending, .cancelling:
                    attempt = 0
                    delayNs = Self.pollMinDelayNs
                    continue
                case .done(let url):
                    guard let url else {
                        return .error(message: "Generation completed but no result was returned.")
                    }
                    return .done(url: url)
                case .error(let message):
                    return .error(message: message)
                case .cancelled:
                    return .cancelled
                }
            } catch is CancellationError {
                throw CancellationError()
            } catch {
                if Task.isCancelled { throw CancellationError() }
                guard !Self.isJobLost(error) else { throw error }
                guard Self.isRetryablePollError(error), attempt < Self.maxPollRetries else {
                    throw error
                }
                if Self.isOfflineError(error) {
                    await waitForNetworkRecovery()
                } else {
                    try await Task.sleep(nanoseconds: delayNs)
                    delayNs = min(Self.pollMaxDelayNs, UInt64(Double(delayNs) * 2))
                }
                attempt += 1
            }
        }
    }

    /// Опрашивает генерацию и затем подтягивает свежую историю чата.
    private func pollAndRefresh(jobID: String) async {
        do {
            let outcome = try await pollGeneration(jobID: jobID)
            switch outcome {
            case .done(let generatedURL):
                try? await refreshChatAfterGenerate(generatedURL: generatedURL)
            case .error(let message):
                sendErrorMessage = message
                try? await refreshChatAfterGenerate(generatedURL: nil)
            case .cancelled:
                sendErrorMessage = nil
                try? await refreshChatAfterGenerate(generatedURL: nil)
            }
        } catch is CancellationError {
            // Таска отменена — не показываем ошибку.
        } catch {
            if Self.isJobLost(error) {
                // job_id живёт в памяти бэкенда и не переживает его перезапуск.
                sendErrorMessage = "Generation status is unavailable. Please refresh the chat."
            } else {
                sendErrorMessage = failureMessage(for: error)
            }
            try? await refreshChatAfterGenerate(generatedURL: nil)
        }
    }

    /// Ждёт восстановления сети до 60 секунд, проверяя раз в секунду.
    private func waitForNetworkRecovery() async {
        if NetworkMonitor.shared.isConnected { return }
        for _ in 0..<60 {
            if Task.isCancelled { return }
            try? await Task.sleep(nanoseconds: 1_000_000_000)
            if NetworkMonitor.shared.isConnected { return }
        }
    }

    private static func isJobLost(_ error: Error) -> Bool {
        if case NetworkError.httpError(let statusCode) = error {
            return statusCode == 404
        }
        return false
    }

    private static func isRetryablePollError(_ error: Error) -> Bool {
        if case NetworkError.httpError(let statusCode) = error {
            return [408, 429, 500, 502, 503, 504].contains(statusCode)
        }
        return error is URLError
    }

    private static func isOfflineError(_ error: Error) -> Bool {
        guard let urlError = error as? URLError else { return false }
        return [
            .notConnectedToInternet,
            .cannotFindHost,
            .cannotConnectToHost,
            .dnsLookupFailed,
            .dataNotAllowed
        ].contains(urlError.code)
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
                id: pending.id,
                type: .user,
                text: pending.text,
                date: pending.createdAt,
                isPending: true
            ))
        }
    }

    private func markMessagePending(id: ChatMessage.ID) {
        updateMessage(id: id) { message in
            message.isPending = true
            message.isFailed = false
        }
    }

    private func markMessageFailed(id: ChatMessage.ID) {
        updateMessage(id: id) { message in
            message.isPending = false
            message.isFailed = true
        }
    }

    private func markPendingMessageFailed(_ pending: ChatCache.PendingMessage) {
        markMessageFailed(id: pending.id)
    }

    private func markPendingMessageSent(_ pending: ChatCache.PendingMessage) {
        updateMessage(id: pending.id) { message in
            message.isPending = false
            message.isFailed = false
        }
    }

    private func removeMessage(id: ChatMessage.ID) {
        messages.removeAll { $0.id == id }
        sections = makeSections(from: messages)
        if lastMessageID == id {
            lastMessageID = messages.last?.id
        }
    }

    private func updateMessage(
        id: ChatMessage.ID,
        update: (inout ChatMessage) -> Void
    ) {
        guard let index = messages.firstIndex(where: { $0.id == id }) else { return }
        update(&messages[index])
        sections = makeSections(from: messages)
    }

    private func queuePendingMessage(
        id: ChatMessage.ID,
        text: String,
        createdAt: Date,
        modelID: String
    ) async {
        pendingMessages.append(ChatCache.PendingMessage(
            id: id,
            text: text,
            createdAt: createdAt,
            modelID: modelID
        ))
        pendingCount = pendingMessages.count
        await ChatCache.shared.savePendingMessages(pendingMessages, for: projectID)
    }

    private func persistSelectedModel() {
        UserDefaults.standard.set(selectedModelID, forKey: Self.selectedModelStorageKey)
    }

    private func shouldKeepPending(after error: Error) -> Bool {
        if case NetworkError.httpError(let statusCode) = error {
            return [408, 429, 500, 502, 503, 504].contains(statusCode)
        }

        guard let urlError = error as? URLError else { return false }
        return [
            .notConnectedToInternet,
            .cannotFindHost,
            .cannotConnectToHost,
            .dnsLookupFailed,
            .dataNotAllowed
        ].contains(urlError.code)
    }

    private func failureMessage(for error: Error) -> String {
        if case NetworkError.httpError(let statusCode) = error {
            return "Unable to send message (server error \(statusCode))."
        }
        if case NetworkError.serverValidation = error {
            return "The server rejected this message."
        }
        if let urlError = error as? URLError,
           [.timedOut, .networkConnectionLost].contains(urlError.code) {
            return "The result is uncertain. Refresh the chat before sending again."
        }
        return "Unable to send message."
    }

    private func reload() async {
        isLoading = true
        isOffline = false
        loadFailed = false

        var activeGeneration: (jobID: String, cancelling: Bool)?
        do {
            async let project = service.fetchProject(id: projectID)
            async let remoteMessages = service.fetchChat(projectID: projectID)
            let (fetchedProject, fetchedMessages) = try await (project, remoteMessages)
            privacy = ProjectPrivacy(isPublic: fetchedProject.isPublic)
            activeGeneration = findActiveGeneration(in: fetchedMessages)
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
        } else if let active = activeGeneration {
            await resumeGeneration(jobID: active.jobID, cancelling: active.cancelling)
        }
    }

    /// Находит активную генерацию по виртуальному assistant-placeholder'у
    /// (status "pending"/"cancelling"), который бэкенд дописывает в историю
    /// чата на время выполнения job'ы. Возвращает job_id и признак отмены.
    private func findActiveGeneration(
        in remoteMessages: [RemoteChatMessage]
    ) -> (jobID: String, cancelling: Bool)? {
        for message in remoteMessages.reversed() {
            guard message.role.lowercased() == "assistant", let jobID = message.jobID else {
                continue
            }
            if message.status == "pending" { return (jobID, false) }
            if message.status == "cancelling" { return (jobID, true) }
        }
        return nil
    }

    private func resumeGeneration(jobID: String, cancelling: Bool) async {
        isGenerating = true
        activeJobID = jobID
        isCancelling = cancelling
        await pollAndRefresh(jobID: jobID)
        isGenerating = false
        activeJobID = nil
        isCancelling = false
    }

    private func applyRemoteMessages(_ remoteMessages: [RemoteChatMessage]) {
        replaceMessages(remoteMessages)
        mergePendingMessages()
    }

    private func mergePendingMessages() {
        for pending in pendingMessages {
            let alreadyPresent = messages.contains { $0.id == pending.id }
            if !alreadyPresent {
                appendMessage(ChatMessage(
                    id: pending.id,
                    type: .user,
                    text: pending.text,
                    date: pending.createdAt,
                    isPending: true
                ))
            }
        }
    }

    private func replaceMessages(_ remoteMessages: [RemoteChatMessage]) {
        let mapped = remoteMessages.compactMap { remoteMessage -> ChatMessage? in
            let isAssistant = remoteMessage.role.lowercased() == "assistant"

            // Бэкенд дописывает в историю виртуальный assistant-placeholder
            // (status "pending"/"cancelling") на время активной генерации —
            // в БД он не хранится. Индикатором служит баббл «Generating…»,
            // поэтому placeholder не рендерим.
            if isAssistant,
               remoteMessage.status == "pending" || remoteMessage.status == "cancelling" {
                return nil
            }

            let attachment: ChatMessageAttachment
            let text: String

            if isAssistant {
                if let versionID = remoteMessage.versionID {
                    attachment = .version(id: versionID, url: versionURLs[versionID])
                    text = "Generated a new version."
                } else if let errorMessage = remoteMessage.error {
                    attachment = .error(message: errorMessage)
                    text = errorMessage
                } else if remoteMessage.status == "cancelled" {
                    attachment = .cancelled
                    text = "Generation cancelled"
                } else {
                    attachment = .none
                    text = "Generated a new version."
                }
            } else {
                attachment = .none
                text = remoteMessage.text ?? ""
            }

            return ChatMessage(
                type: isAssistant ? .assistant : .user,
                text: text,
                date: remoteMessage.createdAt,
                attachment: attachment,
                jobID: remoteMessage.jobID
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
                text: nil,
                versionID: nil,
                error: "Generation service is temporarily unavailable. Please try again later.",
                createdAt: now.addingTimeInterval(-295)
            ),
            RemoteChatMessage(
                role: "user",
                text: "Try a minimalist approach",
                versionID: nil,
                jobID: "preview-job-3",
                createdAt: now.addingTimeInterval(-100)
            ),
            RemoteChatMessage(
                role: "assistant",
                text: nil,
                versionID: nil,
                status: "cancelled",
                jobID: "preview-job-3",
                createdAt: now.addingTimeInterval(-95)
            )
        ]

        viewModel.replaceMessages(messages)
        viewModel.availableModels = [
            .fallback,
            GenerationModel(remoteID: "gpt://openai/gpt-5.6-luna", owner: "OpenAI")
        ]
        viewModel.didLoad = true
        return viewModel
    }
    #endif
}
