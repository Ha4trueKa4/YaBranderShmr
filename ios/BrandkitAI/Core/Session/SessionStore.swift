import Foundation

/// Состояние авторизации приложения.
///
/// Источник правды — токен в Keychain, а не отдельный флаг: «залогинен»
/// означает ровно «токен есть». Гостевые экраны читают `state`, а защищённые
/// действия вызывают `requireAuthentication()` вместо прямой проверки токена.
@MainActor
@Observable
final class SessionStore {
    enum AuthField: Equatable {
        case fullName
        case email
        case password
    }

    enum AuthOperation: Equatable {
        case signIn
        case register
        case yandexSignIn
    }

    enum State: Equatable {
        /// Токен ещё не проверяли — на старте, чтобы не мигнуть экраном входа.
        case unknown
        case signedOut
        case signedIn
    }

    private(set) var state: State = .unknown
    private(set) var isLoading = false
    /// Какая операция крутит спиннер: на экране две кнопки входа, и `isLoading`
    /// сам по себе не говорит, чей индикатор показывать.
    private(set) var activeOperation: AuthOperation?
    private(set) var errorMessage: String?
    private(set) var errorField: AuthField?
    private(set) var lastSuccessfulOperation: AuthOperation?
    private(set) var signInPresentationRequestID = UUID()

    private let tokenStore: TokenStore
    private let networkClient: NetworkClient
    private var authenticationTask: Task<Void, Never>?
    private var authenticationRequestID = UUID()
    private var pendingAuthenticatedAction: (@MainActor () -> Void)?

    /// Данные авторизованного пользователя (id + имя) и доступ к профилям.
    let currentUserService: any CurrentUserService
    let userProfileService: any UserProfileService
    private let yandexAuthService: any YandexAuthorizing

    init(
        networkClient: NetworkClient = NetworkClient.shared,
        currentUserService: (any CurrentUserService)? = nil,
        userProfileService: (any UserProfileService)? = nil,
        yandexAuthService: (any YandexAuthorizing)? = nil
    ) {
        let tokenStore = TokenStore()
        let profileService = userProfileService ?? RemoteUserProfileService(networkClient: networkClient)
        self.tokenStore = tokenStore
        self.networkClient = networkClient
        self.userProfileService = profileService
        self.currentUserService = currentUserService ?? CurrentUserStore(
            tokenStore: tokenStore,
            userProfileService: profileService
        )
        self.yandexAuthService = yandexAuthService ?? YandexAuthService()
    }

    /// Проверка на входе: вызывается один раз при запуске.
    ///
    /// Если токена нет или его срок жизни истёк — разлогиниваемся: очищаем
    /// Keychain и хранилище пользователя, чтобы не показывать «залогинен»
    /// с нерабочим токеном.
    func restore() {
        if let token = tokenStore.accessToken, !token.isEmpty, !JWTDecoder.isExpired(token) {
            state = .signedIn
            Task { await currentUserService.restore() }
        } else {
            signOut()
        }
    }

    func signIn(email: String, password: String) {
        authenticate(
            endpoint: .login,
            body: LoginRequest(email: email, password: password),
            operation: .signIn
        )
    }

    func register(email: String, password: String, nickname: String) {
        authenticate(
            endpoint: .register,
            body: RegisterRequest(email: email, password: password, nickname: nickname),
            operation: .register
        )
    }

    /// SDK-флоу Yandex ID: нативная авторизация → OAuth-токен → обмен на нашу
    /// сессию через POST /auth/yandex. Отмена шторки пользователем — не ошибка.
    func signInWithYandex() {
        guard !isLoading else { return }

        isLoading = true
        activeOperation = .yandexSignIn
        clearError()
        lastSuccessfulOperation = nil
        let requestID = UUID()
        authenticationRequestID = requestID

        authenticationTask = Task { [weak self] in
            guard let self else { return }
            do {
                let token = try await self.yandexAuthService.authorize()
                guard !Task.isCancelled, self.authenticationRequestID == requestID else { return }
                self.isLoading = false
                self.authenticationTask = nil
                self.authenticate(
                    endpoint: .yandexAuth,
                    body: YandexAuthRequest(accessToken: token),
                    operation: .yandexSignIn
                )
            } catch {
                guard !Task.isCancelled, self.authenticationRequestID == requestID else { return }
                self.isLoading = false
                self.activeOperation = nil
                self.authenticationTask = nil
                switch error {
                case YandexAuthError.cancelled:
                    break
                case YandexAuthError.unavailable:
                    self.errorMessage = "Yandex ID sign-in is not available right now."
                default:
                    self.errorMessage = "Yandex authentication failed. Please try again."
                }
            }
        }
    }

    func clearError() {
        errorMessage = nil
        errorField = nil
    }

    /// Возвращает `true`, если защищённое действие можно выполнить сейчас.
    /// Для гостя просит корневой экран показать авторизацию и возвращает `false`.
    @discardableResult
    func requireAuthentication() -> Bool {
        guard state == .signedIn else {
            pendingAuthenticatedAction = nil
            signInPresentationRequestID = UUID()
            return false
        }
        return true
    }

    /// Выполняет защищённое действие сразу, если пользователь залогинен.
    /// Для гостя показывает авторизацию и выполняет действие после успешного
    /// входа; закрытие авторизации без входа сбрасывает отложенное действие
    /// (`cancelPendingAuthentication`).
    func performAuthenticated(_ action: @escaping @MainActor () -> Void) {
        guard state != .signedIn else {
            action()
            return
        }
        pendingAuthenticatedAction = action
        signInPresentationRequestID = UUID()
    }

    /// Корневой экран зовёт это, когда авторизацию закрыли без входа:
    /// отложенное действие не должно выстрелить при позднем логине.
    func cancelPendingAuthentication() {
        pendingAuthenticatedAction = nil
    }

    private func authenticate<Request: Encodable & Sendable>(
        endpoint: APIEndpoint,
        body: Request,
        operation: AuthOperation
    ) {
        guard !isLoading else { return }

        isLoading = true
        activeOperation = operation
        clearError()
        lastSuccessfulOperation = nil
        let requestID = UUID()
        authenticationRequestID = requestID
        let client = networkClient
        let store = tokenStore

        authenticationTask = Task { [weak self, client, store] in
            do {
                let response: AuthResponse = try await client.request(
                    .post,
                    endpoint,
                    authorization: .none,
                    body: body
                )
                guard !response.accessToken.isEmpty else {
                    throw NetworkError.invalidResponse
                }
                guard !Task.isCancelled,
                      let self,
                      self.authenticationRequestID == requestID
                else { return }
                guard store.save(response.accessToken) else {
                    throw NetworkError.tokenStorageError
                }
                self.currentUserService.save(
                    id: response.user.id,
                    name: response.user.nickname,
                    email: response.user.email
                )
                self.lastSuccessfulOperation = operation
                self.state = .signedIn
                self.isLoading = false
                self.activeOperation = nil
                self.authenticationTask = nil
                self.runPendingAuthenticatedAction()
            } catch is CancellationError {
                return
            } catch {
                guard !Task.isCancelled,
                      let self,
                      self.authenticationRequestID == requestID
                else { return }
                let failure = self.failure(for: error, operation: operation)
                self.errorMessage = failure.message
                self.errorField = failure.field
                self.isLoading = false
                self.activeOperation = nil
                self.authenticationTask = nil
            }
        }
    }

    private func runPendingAuthenticatedAction() {
        let action = pendingAuthenticatedAction
        pendingAuthenticatedAction = nil
        action?()
    }

    /// Единственная точка выхода: сюда же придёт интерцептор на 401.
    func signOut() {
        authenticationRequestID = UUID()
        pendingAuthenticatedAction = nil
        authenticationTask?.cancel()
        authenticationTask = nil
        isLoading = false
        activeOperation = nil
        tokenStore.clear()
        currentUserService.clear()
        clearError()
        state = .signedOut
    }

    private func failure(for error: Error, operation: AuthOperation) -> (message: String, field: AuthField?) {
        if case NetworkError.serverValidation(_, let code, let field) = error {
            let resolvedField = authField(for: code ?? field, operation: operation)
            switch code {
            case "EMAIL_EXISTS" where operation == .yandexSignIn:
                // Автопривязки Yandex ID к парольному аккаунту нет намеренно —
                // объясняем общей плашкой, поля email на этом пути нет.
                return ("This email is already registered with a password. Sign in with your password instead.", nil)
            case "EMAIL_EXISTS":
                return ("An account with this email already exists.", .email)
            case "YANDEX_EMAIL_SCOPE_REQUIRED", "INVALID_REQUEST":
                return ("Yandex authentication failed. Please try again.", nil)
            case "YANDEX_OAUTH_FAILED":
                return ("Yandex authentication is temporarily unavailable. Try again later.", nil)
            case "NICKNAME_EXISTS":
                return ("This name is already in use.", .fullName)
            case "INVALID_EMAIL":
                return ("Enter a valid email address.", .email)
            case "INVALID_PASSWORD":
                return ("Choose a stronger password.", .password)
            case "INVALID_NICKNAME":
                return ("Enter a valid name.", .fullName)
            default:
                return ("Please check your details and try again.", resolvedField)
            }
        }

        if case NetworkError.httpError(let statusCode) = error {
            switch statusCode {
            case 401:
                return ("Invalid email or password.", .none)
            case 409:
                return ("Email or name is already in use.", operation == .register ? .email : nil)
            case 400:
                return ("Please check your details and try again.", nil)
            case 500...599:
                return ("The service is temporarily unavailable. Try again later.", nil)
            default:
                return ("Something went wrong. Try again later.", nil)
            }
        }
        if case NetworkError.missingAccessToken = error {
            return ("Please sign in again.", nil)
        }
        if case NetworkError.tokenStorageError = error {
            return ("Unable to save the session. Try again.", nil)
        }
        return ("Unable to connect. Check your internet connection and try again.", nil)
    }

    private func authField(for value: String?, operation: AuthOperation) -> AuthField? {
        switch value?.lowercased() {
        case "email":
            return .email
        case "password":
            return .password
        case "nickname", "name", "fullname":
            return operation == .register ? .fullName : nil
        default:
            return nil
        }
    }
}
