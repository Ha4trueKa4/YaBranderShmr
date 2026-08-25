import CryptoKit
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
            case .privateProject: String(localized: "Private Project")
            case .publicShowcase: String(localized: "Public Showcase")
            }
        }

        var shortTitle: String {
            switch self {
            case .privateProject: String(localized: "Private")
            case .publicShowcase: String(localized: "Public")
            }
        }

        var subtitle: String {
            switch self {
            case .privateProject: String(localized: "Only workspace members can edit and view")
            case .publicShowcase: String(localized: "Index project in public showcase database")
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

    /// Запрос на локальное прекращение ожидания long-poll'а. Ставится в
    /// `cancelGeneration`, когда cancel-запрос до сервера не доходит
    /// (например, офлайн): генерация на сервере продолжает выполняться,
    /// а клиент отвязывается от неё и снова прицепится при `reload()`
    /// (placeholder в истории) или при следующем `generate` (409).
    private var pollAbortRequested = false
    var privacy = ProjectPrivacy.privateProject

    /// Момент, когда job перешла в состояние «cancelling» (сервер принял
    /// отмену либо опрос впервые увидел этот статус). Через
    /// `cancelGracePeriod` после этого клиент отвязывается от опроса —
    /// см. `pollGeneration`.
    private var cancellingSince: ContinuousClock.Instant?

    /// jobID, чей статус сервер уже не отдаёт (404). Не резюмируются при
    /// `reload()` — см. `ChatCache.CachedChat.lostJobIDs`.
    private var lostJobIDs: Set<String> = []

    private let service: any ProjectServiceProtocol
    private var didLoad = false
    private var versionURLs: [UUID: URL] = [:]
    private var pendingMessages: [ChatCache.PendingMessage] = []
    private var queueRetryTask: Task<Void, Never>?
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
                // isOffline сознательно не сбрасываем: плашка гаснет по
                // факту успешного запроса, а не по факту «сеть вроде
                // появилась» — она может и не заработать.
                guard let self, !self.isGenerating else {
                    // Активный опрос сам переживает офлайн через
                    // waitForNetworkRecovery — ничего не дёргаем.
                    return
                }
                if self.pendingMessages.contains(where: { $0.deliveryState == .pending }) {
                    await self.flushPendingMessages()
                } else {
                    // Опрос мог исчерпать дедлайн/ретраи во время офлайна и
                    // отвязаться от активной job — восстанавливаем по истории.
                    await self.recoverAfterNetworkDrop()
                }
            }
        }
    }

    var selectedModel: GenerationModel {
        availableModels.first(where: { $0.id == selectedModelID })
            ?? GenerationModel(remoteID: selectedModelID, owner: nil)
    }

    var hasUnsentMessage: Bool {
        pendingMessages.contains { $0.deliveryState == .pending }
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
            lostJobIDs = cached.lostJobIDs
        }
        await reload()
    }

    func retry() async {
        // Во время генерации reload перезаписал бы историю и затёр летящий
        // optimistic-баббл (его нет ни на сервере, ни в pending-очереди).
        guard !isLoading, !isGenerating else { return }
        didLoad = false
        await reload()
    }

    /// Вызывается из `.task` на ChatView и живёт, пока экран находится в
    /// иерархии. SwiftUI отменяет задачу при закрытии экрана — в этот момент
    /// отвязываемся от опроса генерации: застрявшая на сервере job не должна
    /// держать опрос (и ViewModel) живыми в фоне после ухода из чата.
    func waitForViewRemoval() async {
        while !Task.isCancelled {
            try? await Task.sleep(for: .seconds(60))
        }
        pollAbortRequested = true
    }

    func retryFailedMessage(id: ChatMessage.ID) async {
        guard !isGenerating,
              let message = messages.first(where: { $0.id == id && $0.isFailed })
        else { return }

        let text = message.text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        guard text.count <= Self.maxPromptLength else {
            sendErrorMessage = String(localized: "Message is too long (maximum 5000 characters).")
            return
        }

        pendingMessages.removeAll { $0.id == id }
        updatePendingCount()
        await ChatCache.shared.savePendingMessages(pendingMessages, for: projectID)
        removeMessage(id: id)
        await sendMessage(text, reuseRequestID: id)
    }

    /// `reuseRequestID` сохраняет исходный uuid баббла при повторной
    /// отправке: он уходит на сервер как client_request_id и делает попытки
    /// одного сообщения идемпотентными, когда бэкенд это поддержит.
    func sendMessage(_ text: String, reuseRequestID: UUID? = nil) async {
        let text = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty, !isGenerating else { return }
        guard text.count <= Self.maxPromptLength else {
            sendErrorMessage = String(localized: "Message is too long (maximum 5000 characters).")
            return
        }

        sendErrorMessage = nil
        let chatMessage = ChatMessage(
            id: reuseRequestID ?? UUID(),
            type: .user,
            text: text,
            date: Date()
        )
        let modelID = selectedModel.id
        appendMessage(chatMessage)

        // If the network is already known to be unavailable, persist the
        // message before starting a request so it survives closing the chat.
        guard NetworkMonitor.shared.isConnected else {
            isOffline = true
            markMessagePending(id: chatMessage.id)
            await queuePendingMessage(
                id: chatMessage.id,
                text: text,
                createdAt: chatMessage.date,
                modelID: modelID
            )
            sendErrorMessage = String(localized: "Message is pending and will be sent when the connection recovers.")
            return
        }

        isGenerating = true

        let jobID: String
        do {
            jobID = try await service.generate(
                projectID: projectID,
                prompt: text,
                modelID: modelID,
                clientRequestID: chatMessage.id.uuidString
            )
        } catch NetworkError.generationInProgress(let existingJobID) where lostJobIDs.contains(existingJobID) {
            // Известная зависшая job: сервер продолжит отвечать 409 с её id,
            // ждать её повторно (ещё один grace-таймаут) бессмысленно.
            isGenerating = false
            markMessageFailed(id: chatMessage.id)
            await queuePendingMessage(
                id: chatMessage.id,
                text: text,
                createdAt: chatMessage.date,
                modelID: modelID,
                deliveryState: .failed
            )
            sendErrorMessage = Self.stuckGenerationMessage
            return
        } catch NetworkError.generationInProgress(let existingJobID) {
            removeMessage(id: chatMessage.id)
            restorePrompt = text
            sendErrorMessage = String(localized: "Another generation is in progress. Your message was not sent.")
            activeJobID = existingJobID
            isCancelling = false
            await pollAndRefresh(jobID: existingJobID)
            isGenerating = false
            clearActiveJobState()
            guard !Task.isCancelled else { return }
            await flushPendingMessages()
            return
        } catch {
            isGenerating = false
            if indicatesOffline(error) {
                isOffline = true
            }
            if shouldKeepPending(after: error) {
                markMessagePending(id: chatMessage.id)
                await queuePendingMessage(
                    id: chatMessage.id,
                    text: text,
                    createdAt: chatMessage.date,
                    modelID: modelID,
                    ambiguousSince: Self.isPossiblyDelivered(error) ? Date() : nil
                )
                sendErrorMessage = String(localized: "Message is pending and will be sent when the connection recovers.")
            } else {
                markMessageFailed(id: chatMessage.id)
                await queuePendingMessage(
                    id: chatMessage.id,
                    text: text,
                    createdAt: chatMessage.date,
                    modelID: modelID,
                    deliveryState: .failed
                )
                sendErrorMessage = failureMessage(for: error)
            }
            return
        }

        activeJobID = jobID
        isCancelling = false
        await pollAndRefresh(jobID: jobID)
        isGenerating = false
        clearActiveJobState()
        guard !Task.isCancelled else { return }
        await flushPendingMessages()
    }

    func flushPendingMessages() async {
        guard !isGenerating,
              pendingMessages.contains(where: { $0.deliveryState == .pending })
        else { return }
        isGenerating = true
        defer { isGenerating = false }

        /// Защита от бесконечного цикла 409: после повторного конфликта
        /// ожидание продолжаем только по внешнему триггеру (возврат сети,
        /// переоткрытие чата).
        var consecutiveConflicts = 0
        var networkRetries = 0

        flushLoop: while let pendingIndex = pendingMessages.firstIndex(where: {
            $0.deliveryState == .pending
        }) {
            if Task.isCancelled || pollAbortRequested { break }
            let prompt = pendingMessages[pendingIndex]
            guard prompt.text.count <= Self.maxPromptLength else {
                pendingMessages[pendingIndex].deliveryState = .failed
                updatePendingCount()
                await ChatCache.shared.savePendingMessages(pendingMessages, for: projectID)
                markPendingMessageFailed(prompt)
                continue
            }

            // Попытка с неизвестным исходом (ответ потерян, но сервер мог
            // принять запрос): перед повторным POST сверяемся с историей —
            // вслепую можно запустить вторую генерацию с тем же промптом.
            if prompt.ambiguousSince != nil {
                switch await reconcileAmbiguousDelivery(prompt) {
                case .delivered:
                    continue
                case .notDelivered:
                    break
                case .unavailable:
                    // Историю прочитать не удалось: отправлять вслепую
                    // нельзя, очередь дождётся следующего триггера.
                    break flushLoop
                }
            }

            let jobID: String
            do {
                jobID = try await service.generate(
                    projectID: projectID,
                    prompt: prompt.text,
                    modelID: prompt.modelID,
                    clientRequestID: prompt.id.uuidString
                )
            } catch NetworkError.generationInProgress(let existingJobID)
                where lostJobIDs.contains(existingJobID) {
                // Известная зависшая job: не цепляемся к ней повторно —
                // честная ошибка вместо вечного pending в очереди.
                pendingMessages[pendingIndex].deliveryState = .failed
                updatePendingCount()
                await ChatCache.shared.savePendingMessages(pendingMessages, for: projectID)
                markPendingMessageFailed(prompt)
                sendErrorMessage = Self.stuckGenerationMessage
                break
            } catch NetworkError.generationInProgress(let existingJobID) {
                activeJobID = existingJobID
                isCancelling = false
                await pollAndRefresh(jobID: existingJobID)
                clearActiveJobState()
                // Блокирующая job только что завершилась — сразу пробуем
                // отправить следующее сообщение, но не бесконечно.
                consecutiveConflicts += 1
                if consecutiveConflicts >= 2 { break }
                continue
            } catch {
                if shouldKeepPending(after: error) {
                    // Очередь остаётся на потом; если сеть на самом деле
                    // пропала — показываем плашку офлайна.
                    if indicatesOffline(error) {
                        isOffline = true
                    }
                    // Попытка могла дойти до сервера — следующая отправка
                    // обязана свериться с историей, прежде чем POSTить снова.
                    if Self.isPossiblyDelivered(error),
                       let index = pendingMessages.firstIndex(where: { $0.id == prompt.id }) {
                        pendingMessages[index].ambiguousSince = Date()
                        await ChatCache.shared.savePendingMessages(pendingMessages, for: projectID)
                    }
                    // Путь уже поднялся, а запрос упал (сеть «едет» после
                    // VPN-переключения): без повторов очередь до рестарта
                    // никто больше не дёрнет — нотификация приходит один раз.
                    if networkRetries < Self.networkRetryDelays.count,
                       NetworkMonitor.shared.isConnected {
                        try? await Task.sleep(for: Self.networkRetryDelays[networkRetries])
                        networkRetries += 1
                        if Task.isCancelled || pollAbortRequested { break }
                        continue
                    }
                    break
                }
                pendingMessages[pendingIndex].deliveryState = .failed
                updatePendingCount()
                await ChatCache.shared.savePendingMessages(pendingMessages, for: projectID)
                markPendingMessageFailed(prompt)
                sendErrorMessage = failureMessage(for: error)
                continue
            }

            consecutiveConflicts = 0
            networkRetries = 0
            pendingMessages.remove(at: pendingIndex)
            updatePendingCount()
            // Пишем на диск сразу: pollAndRefresh может идти минутами, а
            // незаписанное удаление превращается в повторную отправку уже
            // принятого сообщения после перезапуска приложения.
            await ChatCache.shared.savePendingMessages(pendingMessages, for: projectID)
            sendErrorMessage = nil
            activeJobID = jobID
            isCancelling = false
            await pollAndRefresh(jobID: jobID)
            clearActiveJobState()
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

    func restoreUnsentMessage() async {
        guard !isGenerating,
              let pending = pendingMessages.last(where: { $0.deliveryState == .pending })
                  ?? pendingMessages.last(where: { $0.deliveryState == .failed })
        else { return }

        pendingMessages.removeAll { $0.id == pending.id }
        updatePendingCount()
        await ChatCache.shared.savePendingMessages(pendingMessages, for: projectID)
        removeMessage(id: pending.id)
        sendErrorMessage = nil
        restorePrompt = pending.text
    }

    /// Исход сверки неоднозначной отправки с серверной историей.
    private enum AmbiguousDeliveryResolution {
        /// Сервер уже принял это сообщение (видно в истории): из очереди
        /// удалено, история обновлена, активная job подхвачена опросом.
        case delivered
        /// В истории сообщения нет — безопасно отправлять.
        case notDelivered
        /// Историю прочитать не удалось — отправлять вслепую нельзя.
        case unavailable
    }

    /// Сверяет попытку с неизвестным исходом (ответ потерян после того, как
    /// запрос мог дойти) с серверной историей. Бэкенд пишет user-сообщение
    /// в chat_history синхронно с приёмом job (с job_id, ещё до ответа 202),
    /// поэтому принятый запрос всегда оставляет след. Окно — с запасом на
    /// расхождение часов; ложное срабатывание требует отправки двух
    /// одинаковых промптов внутри окна — редкий случай с малой ценой ошибки.
    private func reconcileAmbiguousDelivery(
        _ pending: ChatCache.PendingMessage
    ) async -> AmbiguousDeliveryResolution {
        guard let attemptedAt = pending.ambiguousSince else { return .notDelivered }

        let remoteMessages: [RemoteChatMessage]
        do {
            remoteMessages = try await service.fetchChat(projectID: projectID)
        } catch {
            if indicatesOffline(error) {
                isOffline = true
            }
            return .unavailable
        }

        let windowStart = attemptedAt.addingTimeInterval(-Self.ambiguousDeliverySkew)
        let delivered = remoteMessages.contains { message in
            message.role.lowercased() == "user"
                && message.jobID != nil
                && message.text == pending.text
                && message.createdAt >= windowStart
        }
        guard delivered else { return .notDelivered }

        if let index = pendingMessages.firstIndex(where: { $0.id == pending.id }) {
            pendingMessages.remove(at: index)
            updatePendingCount()
            await ChatCache.shared.savePendingMessages(pendingMessages, for: projectID)
        }
        applyRemoteMessages(remoteMessages)
        if isOffline { isOffline = false }
        await ChatCache.shared.save(
            projectID: projectID,
            messages: remoteMessages,
            isPublic: privacy == .publicShowcase
        )
        if let active = findActiveGeneration(in: remoteMessages, excluding: lostJobIDs) {
            // Принятая job ещё выполняется — подхватываем её опросом, как
            // ветка 409 выше (resumeGeneration не годится: он guard'ится по
            // isGenerating, который уже держит flush).
            activeJobID = active.jobID
            isCancelling = active.cancelling
            cancellingSince = active.cancelling ? ContinuousClock.now : nil
            await pollAndRefresh(jobID: active.jobID)
            clearActiveJobState()
        }
        return .delivered
    }

    /// Возврат сети после того, как опрос активной генерации отвязался
    /// (исчерпаны дедлайн/ретраи во время офлайна): подтягиваем историю и
    /// возобновляем найденную активную job, не дожидаясь ручного reload
    /// или переоткрытия чата.
    private func recoverAfterNetworkDrop() async {
        guard !isGenerating, !isLoading, !pollAbortRequested else { return }
        guard let remoteMessages = try? await service.fetchChat(projectID: projectID) else {
            return
        }
        applyRemoteMessages(remoteMessages)
        if isOffline { isOffline = false }
        await ChatCache.shared.save(
            projectID: projectID,
            messages: remoteMessages,
            isPublic: privacy == .publicShowcase
        )
        if let active = findActiveGeneration(in: remoteMessages, excluding: lostJobIDs) {
            await resumeGeneration(jobID: active.jobID, cancelling: active.cancelling)
        }
    }

    private func clearActiveJobState() {
        activeJobID = nil
        isCancelling = false
        cancellingSince = nil
    }

    func cancelGeneration() async {
        guard let jobID = activeJobID, !isCancelling else { return }
        isCancelling = true
        do {
            let status = try await service.cancelGeneration(projectID: projectID, jobID: jobID)
            if case .cancelled = status {
                // Сервер уже завершил job — выходим из опроса коротким путём.
                pollAbortRequested = true
            } else {
                cancellingSince = ContinuousClock.now
            }
        } catch {
            isCancelling = false
            // Cancel-запрос не дошёл до сервера — не держим пользователя
            // в ожидании: прерываем опрос локально (см. pollAbortRequested).
            pollAbortRequested = true
        }
    }

    func dateTitle(for date: Date) -> String {
        let calendar = Calendar.current

        if calendar.isDateInToday(date) { return String(localized: "Today") }
        if calendar.isDateInYesterday(date) { return String(localized: "Yesterday") }
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
    private static let pollMinDelay: Duration = .seconds(1)
    private static let pollMaxDelay: Duration = .seconds(15)

    /// Суммарный бюджет ожидания одной job. Ограничивает худшие сценарии
    /// (умножение retry-пауз, длинный офлайн) — по исчерпании клиент
    /// отвязывается от опроса и освобождает ввод; результат подхватит
    /// placeholder в истории при следующем `reload()`.
    private static let pollDeadline: Duration = .seconds(5 * 60)

    /// Минимальный интервал между соседними запросами статуса. Настоящий
    /// long-poll держит соединение десятки секунд; мгновенный ответ —
    /// признак деградации сервера, без этой паузы клиент уходит в hot-loop.
    private static let pollMinRequestInterval: Duration = .seconds(1)

    /// Сколько ждать терминального статуса после того, как job вошла в
    /// «cancelling». На живом деплое cancel возвращает 200, но статус может
    /// оставаться «cancelling» неограниченно долго — держать ввод
    /// заблокированным всё это время нельзя.
    private static let cancelGracePeriod: Duration = .seconds(30)

    /// Сервер отвечает 409 с id job, уже пережившей grace-таймаут
    /// (см. lostJobIDs): новые генерации проекта заблокированы зависшей
    /// job — ждём её повторно бессмысленно, честно сообщаем об этом.
    private static var stuckGenerationMessage: String {
        String(localized: "The server is busy with a stuck generation. Please try again in a few minutes.")
    }

    /// Паузы повторных попыток отправки, когда сетевой путь уже поднялся,
    /// а запросы ещё падают (сеть «едет» после VPN-переключений).
    private static let networkRetryDelays: [Duration] = [.seconds(3), .seconds(6), .seconds(12)]

    /// Запас на расхождение часов устройства и сервера при сверке
    /// неоднозначной отправки с историей чата.
    private static let ambiguousDeliverySkew: TimeInterval = 15 * 60

    /// Паузы повторных вычиток истории после терминального статуса job:
    /// статус появляется в Redis раньше записи в chat_history, а чтение
    /// истории идёт со слейва (контракт — android/API.md).
    private static let historyRefetchDelays: [Duration] = [.seconds(0.5), .seconds(1), .seconds(2)]

    /// Автоповтор очереди, когда событие «сеть вернулась» не наступит
    /// (сервер отвечал 408/429/5xx при живом сетевом пути): ограниченный
    /// backoff, дальше — только сеть/плашка/переоткрытие чата.
    private static let queueRetryDelays: [Duration] = [.seconds(10), .seconds(30), .seconds(60)]

    /// Опрашивает статус фоновой задачи генерации через long-polling.
    /// При `pending`/`cancelling` переоткрывает соединение не чаще, чем раз
    /// в секунду. При разрыве делает retry с backoff, при оффлайне ждёт
    /// восстановления. После ~10 неудачных попыток, 404 (задача потеряна)
    /// или исчерпания общего дедлайна завершается ошибкой.
    /// Прерывается локально (без ошибки) по `pollAbortRequested` — см. поле выше.
    private func pollGeneration(jobID: String) async throws -> GenerationOutcome {
        pollAbortRequested = false
        var attempt = 0
        var delay = Self.pollMinDelay
        let deadline = ContinuousClock.now + Self.pollDeadline

        while true {
            if pollAbortRequested {
                pollAbortRequested = false
                return .cancelled
            }
            try Task.checkCancellation()
            if ContinuousClock.now >= deadline {
                return .error(
                    message: String(localized: "Generation is taking too long. Please refresh the chat in a moment.")
                )
            }
            if let cancellingSince,
               ContinuousClock.now >= cancellingSince + Self.cancelGracePeriod {
                // Job в «cancelling» дольше grace-периода: сервер отмену
                // принял, но до терминального статуса не довёл (замечено на
                // живом деплое — статус остаётся «cancelling» бесконечно).
                // Отвязываемся локально: результат, если сервер его всё же
                // доделает, подтянет следующий reload(). Job запоминаем как
                // «не резюмировать», чтобы каждое открытие чата не ждало
                // grace-период заново.
                lostJobIDs.insert(jobID)
                await ChatCache.shared.markJobLost(jobID, projectID: projectID)
                return .cancelled
            }

            do {
                let requestStart = ContinuousClock.now
                let status = try await service.generateStatus(projectID: projectID, jobID: jobID)
                // Пишем только при изменении: @Observable инвалидирует
                // SwiftUI при любой записи, а этот код выполняется на каждом
                // опросе.
                if isOffline { isOffline = false }
                switch status {
                case .pending:
                    attempt = 0
                    delay = Self.pollMinDelay
                    let elapsed = ContinuousClock.now - requestStart
                    if elapsed < Self.pollMinRequestInterval {
                        try await Task.sleep(for: Self.pollMinRequestInterval - elapsed)
                    }
                    continue
                case .cancelling:
                    attempt = 0
                    delay = Self.pollMinDelay
                    if cancellingSince == nil {
                        cancellingSince = ContinuousClock.now
                    }
                    let elapsed = ContinuousClock.now - requestStart
                    if elapsed < Self.pollMinRequestInterval {
                        try await Task.sleep(for: Self.pollMinRequestInterval - elapsed)
                    }
                    continue
                case .done(let url):
                    guard let url else {
                        return .error(
                            message: String(localized: "Generation completed but no result was returned.")
                        )
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
                    isOffline = true
                    await waitForNetworkRecovery()
                } else {
                    try await Task.sleep(for: delay)
                    delay = min(Self.pollMaxDelay, delay * 2)
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
                try? await refreshChatAfterGenerate(jobID: jobID, generatedURL: generatedURL)
            case .error(let message):
                sendErrorMessage = message
                try? await refreshChatAfterGenerate(jobID: jobID, generatedURL: nil)
            case .cancelled:
                sendErrorMessage = nil
                try? await refreshChatAfterGenerate(jobID: jobID, generatedURL: nil)
            }
        } catch is CancellationError {
            // Таска отменена — не показываем ошибку.
        } catch {
            if Self.isJobLost(error) {
                // job_id живёт в памяти бэкенда и не переживает его перезапуск.
                // Запоминаем потерянную job, чтобы каждое последующее
                // открытие чата не заканчивалось этой ошибкой снова.
                lostJobIDs.insert(jobID)
                await ChatCache.shared.markJobLost(jobID, projectID: projectID)
                sendErrorMessage = String(localized: "Generation status is unavailable. Please refresh the chat.")
            } else {
                sendErrorMessage = failureMessage(for: error)
            }
            try? await refreshChatAfterGenerate(jobID: jobID, generatedURL: nil)
        }
    }

    /// Ждёт восстановления сети до 60 секунд, проверяя раз в секунду.
    /// Суммарное ожидание ограничено общим дедлайном опроса.
    private func waitForNetworkRecovery() async {
        if NetworkMonitor.shared.isConnected { return }
        for _ in 0..<60 {
            if Task.isCancelled || pollAbortRequested { return }
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

    /// Read-шаг после завершённой генерации: подтягивает свежую историю
    /// чата, маппит URL новой версии, разогревает кеш SVG и сохраняет
    /// состояние. Ошибки `fetchChat` здесь некритичны — генерация уже
    /// принята бэкендом, поэтому вызывающий снимает промпт с очереди до
    /// этого вызова.
    private func refreshChatAfterGenerate(jobID: String, generatedURL: URL?) async throws {
        // Бэкенд делает статус терминальным в Redis ДО записи результата в
        // chat_history (Postgres, best-effort), а история читается со слейва —
        // первая вычитка может не содержать сообщения завершившейся job
        // (контракт описан в android/API.md). Перечитываем с паузами, пока
        // запись не появится; не дождавшись — ленту не подменяем: пустая
        // история стёрла бы и промпт, не показав результат.
        var remoteMessages = try await service.fetchChat(projectID: projectID)

        if !Self.hasTerminalRecord(jobID: jobID, in: remoteMessages) {
            for delay in Self.historyRefetchDelays {
                try? await Task.sleep(for: delay)
                if Task.isCancelled { return }
                remoteMessages = try await service.fetchChat(projectID: projectID)
                if Self.hasTerminalRecord(jobID: jobID, in: remoteMessages) { break }
            }
        }

        if isOffline { isOffline = false }

        guard Self.hasTerminalRecord(jobID: jobID, in: remoteMessages) else {
            // Записи об исходе на сервере может не быть вообще (best-effort).
            // Ленту не трогаем: скелетон снимет isGenerating, а текст ошибки
            // уже показал pollAndRefresh.
            return
        }

        if let generatedURL {
            // Версия определяется строго по сообщению завершившейся job:
            // «последняя версия истории» могла появиться от другой генерации
            // (другой клиент) между завершением job и этим fetchChat — ей
            // нельзя назначать чужой URL и греть под неё кеш.
            let versionID = remoteMessages.reversed().first {
                $0.jobID == jobID && $0.versionID != nil
            }?.versionID
            if let versionID {
                versionURLs[versionID] = generatedURL
                let cacheKey = SVGCache.versionKey(projectID: projectID, versionID: versionID)
                _ = try? await SVGCache.shared.data(for: generatedURL, key: cacheKey)
            }
        }

        applyRemoteMessages(remoteMessages)
        await ChatCache.shared.save(
            projectID: projectID,
            messages: remoteMessages,
            isPublic: privacy == .publicShowcase
        )
    }

    /// Терминальная запись job в истории: assistant-сообщение с этим jobID,
    /// не являющееся живым placeholder'ом (status pending/cancelling).
    /// Формы записей — append_assistant_*.sql на бэкенде: done = version_id,
    /// error = error, cancelled = status "cancelled"; user-сообщения job
    /// (AppendUserMessage) отбрасываются требованием роли assistant.
    private static func hasTerminalRecord(
        jobID: String,
        in messages: [RemoteChatMessage]
    ) -> Bool {
        messages.contains { message in
            message.role.lowercased() == "assistant"
                && message.jobID == jobID
                && message.status != "pending"
                && message.status != "cancelling"
        }
    }

    private func restorePendingMessages(_ messages: [ChatCache.PendingMessage]) {
        pendingMessages = messages
        updatePendingCount()
        for pending in messages {
            appendMessage(ChatMessage(
                id: pending.id,
                type: .user,
                text: pending.text,
                date: pending.createdAt,
                isPending: pending.deliveryState == .pending,
                isFailed: pending.deliveryState == .failed
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
        modelID: String,
        deliveryState: ChatCache.PendingMessage.DeliveryState = .pending,
        ambiguousSince: Date? = nil
    ) async {
        pendingMessages.append(ChatCache.PendingMessage(
            id: id,
            text: text,
            createdAt: createdAt,
            modelID: modelID,
            deliveryState: deliveryState,
            ambiguousSince: ambiguousSince
        ))
        updatePendingCount()
        await ChatCache.shared.savePendingMessages(pendingMessages, for: projectID)
        scheduleQueueRetry()
    }

    /// Отложенный автоповтор очереди: flush дёргается не только событием
    /// возврата сети, которого может не быть (NWPathMonitor считал путь
    /// живым, а сервер отвечал 408/429/5xx). Одно ограниченное расписание
    /// на постановку в очередь — без перезапуска из flush, иначе цепочка
    /// попыток становилась бы бесконечной.
    private func scheduleQueueRetry() {
        guard queueRetryTask == nil else { return }
        queueRetryTask = Task { [weak self] in
            defer { self?.queueRetryTask = nil }
            for delay in Self.queueRetryDelays {
                try? await Task.sleep(for: delay)
                guard let self, !Task.isCancelled, !self.pollAbortRequested else { return }
                guard self.pendingMessages.contains(where: { $0.deliveryState == .pending }) else {
                    return
                }
                if !self.isGenerating {
                    await self.flushPendingMessages()
                }
            }
        }
    }

    private func updatePendingCount() {
        pendingCount = pendingMessages.count { $0.deliveryState == .pending }
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
            .dataNotAllowed,
            // Разрыв соединения транзиентен по природе (типичен при
            // VPN-переключениях) — сообщение должно ждать сеть, а не
            // помечаться «not sent» после первой же неудачи.
            .networkConnectionLost,
            // Таймаут не означает отказ сервера: запрос мог быть принят,
            // а ответ потерян. Такая попытка помечается ambiguousSince
            // (isPossiblyDelivered) и перед повторным POST сверяется
            // с историей — иначе ручной retry запускал бы вторую генерацию.
            .timedOut
        ].contains(urlError.code)
    }

    /// Ошибки, при которых сервер мог успеть принять POST до разрыва:
    /// повторная отправка без сверки с историей рискует второй генерацией.
    private static func isPossiblyDelivered(_ error: Error) -> Bool {
        guard let urlError = error as? URLError else { return false }
        return urlError.code == .networkConnectionLost || urlError.code == .timedOut
    }

    private func indicatesOffline(_ error: Error) -> Bool {
        guard !NetworkMonitor.shared.isConnected else { return false }

        guard let urlError = error as? URLError else { return false }
        return [
            .notConnectedToInternet,
            .cannotFindHost,
            .cannotConnectToHost,
            .dnsLookupFailed,
            .dataNotAllowed,
            .networkConnectionLost
        ].contains(urlError.code)
    }

    private func failureMessage(for error: Error) -> String? {
        if case NetworkError.httpError(let statusCode) = error {
            return String(localized: "Unable to send message (server error \(statusCode)).")
        }
        if case NetworkError.serverValidation = error {
            return String(localized: "The server rejected this message.")
        }
        if let urlError = error as? URLError,
           [.timedOut, .networkConnectionLost].contains(urlError.code) {
            return nil
        }
        return String(localized: "Unable to send message.")
    }

    private func reload() async {
        isLoading = true
        if isOffline { isOffline = false }
        loadFailed = false

        var activeGeneration: (jobID: String, cancelling: Bool)?
        do {
            async let project = service.fetchProject(id: projectID)
            async let remoteMessages = service.fetchChat(projectID: projectID)
            let (fetchedProject, fetchedMessages) = try await (project, remoteMessages)
            privacy = ProjectPrivacy(isPublic: fetchedProject.isPublic)
            activeGeneration = findActiveGeneration(
                in: fetchedMessages,
                excluding: lostJobIDs
            )
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

        if pendingMessages.contains(where: { $0.deliveryState == .pending }) {
            await flushPendingMessages()
        } else if let active = activeGeneration {
            await resumeGeneration(jobID: active.jobID, cancelling: active.cancelling)
        }
    }

    /// Находит активную генерацию по виртуальному assistant-placeholder'у
    /// (status "pending"/"cancelling"), который бэкенд дописывает в историю
    /// чата на время выполнения job'ы. Возвращает job_id и признак отмены.
    /// Потерянные job (404 в прошлом) пропускаются — их опрос бессмыслен.
    private func findActiveGeneration(
        in remoteMessages: [RemoteChatMessage],
        excluding lostJobIDs: Set<String>
    ) -> (jobID: String, cancelling: Bool)? {
        for message in remoteMessages.reversed() {
            guard message.role.lowercased() == "assistant", let jobID = message.jobID else {
                continue
            }
            guard !lostJobIDs.contains(jobID) else { continue }
            if message.status == "pending" { return (jobID, false) }
            if message.status == "cancelling" { return (jobID, true) }
        }
        return nil
    }

    private func resumeGeneration(jobID: String, cancelling: Bool) async {
        guard !isGenerating else { return }
        isGenerating = true
        activeJobID = jobID
        isCancelling = cancelling
        if cancelling {
            // Чат открыт с уже отменяемой job (placeholder «cancelling»):
            // то же терпение, что и после нажатия Cancel, — от момента
            // открытия, а не до общего дедлайна опроса.
            cancellingSince = ContinuousClock.now
        }
        await pollAndRefresh(jobID: jobID)
        isGenerating = false
        clearActiveJobState()
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
                    isPending: pending.deliveryState == .pending,
                    isFailed: pending.deliveryState == .failed
                ))
            }
        }
    }

    private func replaceMessages(_ remoteMessages: [RemoteChatMessage]) {
        let mapped = remoteMessages.enumerated().compactMap { index, remoteMessage -> ChatMessage? in
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
                    text = String(localized: "Generated a new version.")
                } else if let errorMessage = remoteMessage.error {
                    attachment = .error(message: errorMessage)
                    text = errorMessage
                } else if remoteMessage.status == "cancelled" {
                    attachment = .cancelled
                    text = String(localized: "Generation cancelled")
                } else {
                    attachment = .none
                    text = String(localized: "Generated a new version.")
                }
            } else {
                attachment = .none
                text = remoteMessage.text ?? ""
            }

            return ChatMessage(
                id: Self.stableMessageID(index: index, remoteMessage),
                type: isAssistant ? .assistant : .user,
                text: text,
                date: remoteMessage.createdAt,
                attachment: attachment,
                jobID: remoteMessage.jobID
            )
        }
        applyMessages(mapped)
    }

    private func applyMessages(_ newMessages: [ChatMessage]) {
        messages = newMessages
        sections = makeSections(from: newMessages)
        lastMessageID = newMessages.last?.id
    }

    /// Наполненный демо-чат для онбординга: без сети, `load()` сразу выходит
    /// из-за выставленного `didLoad`.
    static func makeDemo(projectID: UUID = UUID()) -> ChatViewModel {
        let viewModel = ChatViewModel(projectID: projectID)
        let now = Date()

        viewModel.applyMessages([
            ChatMessage(
                type: .user,
                text: String(localized: "Make the background darker"),
                date: now.addingTimeInterval(-60)
            ),
            ChatMessage(
                type: .assistant,
                text: String(localized: "Generated a new version."),
                date: now.addingTimeInterval(-59),
                attachment: .mock
            ),
        ])
        viewModel.privacy = .privateProject
        viewModel.didLoad = true
        return viewModel
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

    /// Детерминированный id remote-сообщения: идентичности строк ForEach не
    /// должны меняться при каждой замене истории, иначе каждый refresh
    /// пересоздаёт все бабблы и WKWebView вложений (рост памяти).
    /// Индекс — защита от дублей (одинаковый текст в одну секунду).
    private static func stableMessageID(index: Int, _ message: RemoteChatMessage) -> UUID {
        let seed = [
            message.role,
            String(message.createdAt.timeIntervalSinceReferenceDate),
            message.versionID?.uuidString ?? "",
            message.jobID ?? "",
            message.error ?? "",
            message.text ?? "",
            message.status ?? "",
            String(index),
        ].joined(separator: "\u{1f}")
        let bytes = Array(SHA256.hash(data: Data(seed.utf8)).prefix(16))
        return UUID(uuid: (
            bytes[0], bytes[1], bytes[2], bytes[3],
            bytes[4], bytes[5], bytes[6], bytes[7],
            bytes[8], bytes[9], bytes[10], bytes[11],
            bytes[12], bytes[13], bytes[14], bytes[15]
        ))
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
