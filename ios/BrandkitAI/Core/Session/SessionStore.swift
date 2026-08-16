import Foundation

/// Состояние авторизации приложения.
///
/// Источник правды — токен в Keychain, а не отдельный флаг: «залогинен»
/// означает ровно «токен есть». Экраны состояние не проверяют, они только
/// вызывают методы ниже, а корневой вью реагирует на изменение.
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
    }

    enum State: Equatable {
        /// Токен ещё не проверяли — на старте, чтобы не мигнуть экраном входа.
        case unknown
        case signedOut
        case signedIn
    }

    private(set) var state: State = .unknown
    private(set) var isLoading = false
    private(set) var errorMessage: String?
    private(set) var errorField: AuthField?
    private(set) var lastSuccessfulOperation: AuthOperation?

    private let tokenStore: TokenStore
    private let networkClient: NetworkClient
    private var authenticationTask: Task<Void, Never>?
    private var authenticationRequestID = UUID()

    /// Данные авторизованного пользователя (id + имя) и доступ к профилям.
    let currentUserService: any CurrentUserService
    let userProfileService: any UserProfileService

    init(
        networkClient: NetworkClient = NetworkClient.shared,
        currentUserService: (any CurrentUserService)? = nil,
        userProfileService: (any UserProfileService)? = nil
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

    func clearError() {
        errorMessage = nil
        errorField = nil
    }

    private func authenticate<Request: Encodable & Sendable>(
        endpoint: APIEndpoint,
        body: Request,
        operation: AuthOperation
    ) {
        guard !isLoading else { return }

        isLoading = true
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
                self.currentUserService.save(id: response.user.id, name: response.user.nickname)
                self.lastSuccessfulOperation = operation
                self.state = .signedIn
                self.isLoading = false
                self.authenticationTask = nil
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
                self.authenticationTask = nil
            }
        }
    }

    /// Единственная точка выхода: сюда же придёт интерцептор на 401.
    func signOut() {
        authenticationRequestID = UUID()
        authenticationTask?.cancel()
        authenticationTask = nil
        isLoading = false
        tokenStore.clear()
        currentUserService.clear()
        clearError()
        state = .signedOut
    }

    private func failure(for error: Error, operation: AuthOperation) -> (message: String, field: AuthField?) {
        if case NetworkError.serverValidation(_, let code, let field) = error {
            let resolvedField = authField(for: code ?? field, operation: operation)
            switch code {
            case "EMAIL_EXISTS":
                return ("An account with this email already exists.", .email)
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
