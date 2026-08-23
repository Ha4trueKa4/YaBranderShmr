import Foundation

nonisolated struct TokenStore: Sendable {
    private let item = KeychainItem.accessToken

    var accessToken: String? {
        get { item.value }
        nonmutating set { item.value = newValue }
    }

    @discardableResult
    func save(_ token: String) -> Bool {
        accessToken = token
        return accessToken == token
    }

    func clear() {
        accessToken = nil
    }
}
