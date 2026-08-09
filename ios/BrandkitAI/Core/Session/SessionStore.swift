import Foundation

@MainActor
@Observable
final class SessionStore {
    enum State: Equatable {
        case unknown
        case signedOut
        case signedIn
    }

    private(set) var state: State = .unknown

    private let tokenStore = TokenStore()

    func restore() {
        state = tokenStore.accessToken == nil ? .signedOut : .signedIn
    }

    func signIn(email: String, password: String) {
        tokenStore.accessToken = "stub"
        state = .signedIn
    }

    func register(email: String, password: String, nickname: String) {
        tokenStore.accessToken = "stub"
        state = .signedIn
    }

    func signOut() {
        tokenStore.clear()
        state = .signedOut
    }
}
