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

    private let tokenStore = TokenStore()

    /// Проверка на входе: вызывается один раз при запуске.
    func restore() {
        state = tokenStore.accessToken == nil ? .signedOut : .signedIn
    }

    /// - Note: Заглушка — сетевой слой ещё не подключён.
    func signIn(email: String, password: String) {
        tokenStore.accessToken = "stub"
        state = .signedIn
    }

    /// - Note: Заглушка — сетевой слой ещё не подключён.
    func register(email: String, password: String, nickname: String) {
        tokenStore.accessToken = "stub"
        state = .signedIn
    }

    /// Единственная точка выхода: сюда же придёт интерцептор на 401.
    func signOut() {
        tokenStore.clear()
        state = .signedOut
    }
}
