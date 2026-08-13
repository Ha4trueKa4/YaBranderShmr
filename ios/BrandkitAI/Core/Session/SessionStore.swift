import Foundation

/// Состояние авторизации приложения.
///
/// Источник правды — токен в Keychain, а не отдельный флаг: «залогинен»
/// означает ровно «токен есть». Экраны состояние не проверяют, они только
/// вызывают методы ниже, а корневой вью реагирует на изменение.
@MainActor
@Observable
final class SessionStore {
    enum State: Equatable {
        /// Токен ещё не проверяли — на старте, чтобы не мигнуть экраном входа.
        case unknown
        case signedOut
        case signedIn
    }

    private(set) var state: State = .unknown
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    private let tokenStore = TokenStore()
    private let networkClient: NetworkClient
    private var authenticationTask: Task<Void, Never>?
    private var authenticationRequestID = UUID()

    init(baseURL: String = "http://158.160.187.118:8080/api/v1") {
        networkClient = NetworkClient(baseURL: baseURL, tokenStore: tokenStore)
    }

    /// Проверка на входе: вызывается один раз при запуске.
    func restore() {
        state = tokenStore.accessToken?.isEmpty == false ? .signedIn : .signedOut
    }

    func signIn(email: String, password: String) {
        authenticate(
            path: "/auth/login",
            body: LoginRequest(email: email, password: password)
        )
    }

    func register(email: String, password: String, nickname: String) {
        authenticate(
            path: "/auth/register",
            body: RegisterRequest(email: email, password: password, nickname: nickname)
        )
    }

    private func authenticate<Request: Encodable & Sendable>(path: String, body: Request) {
        guard !isLoading else { return }

        isLoading = true
        errorMessage = nil
        let requestID = UUID()
        authenticationRequestID = requestID
        let client = networkClient
        let store = tokenStore

        authenticationTask = Task { [weak self, client, store] in
            do {
                let response: AuthResponse = try await client.request(
                    .post,
                    path: path,
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
                self.errorMessage = self.message(for: error)
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
        errorMessage = nil
        state = .signedOut
    }

    private func message(for error: Error) -> String {
        if case NetworkError.httpError(let statusCode) = error {
            switch statusCode {
            case 401:
                return "Invalid email or password"
            case 409:
                return "Email or nickname is already in use"
            default:
                return "Server error (\(statusCode))"
            }
        }
        if case NetworkError.missingAccessToken = error {
            return "Please sign in again"
        }
        if case NetworkError.tokenStorageError = error {
            return "Unable to save the session"
        }
        return "Unable to connect to the server"
    }
}
