import Foundation
import Security

/// Хранилище токена доступа в Keychain.
///
/// Keychain, а не UserDefaults: токен — секрет, и он не должен утекать
/// в бэкапы и логи вместе с обычными настройками.
nonisolated struct TokenStore: Sendable {
    private let service = "ai.brandkit.auth"
    private let account = "accessToken"

    var accessToken: String? {
        get {
            let query: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: service,
                kSecAttrAccount as String: account,
                kSecReturnData as String: true,
                kSecMatchLimit as String: kSecMatchLimitOne
            ]

            var item: CFTypeRef?
            guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
                  let data = item as? Data
            else {
                return nil
            }

            return String(data: data, encoding: .utf8)
        }

        nonmutating set {
            let query: [String: Any] = [
                kSecClass as String: kSecClassGenericPassword,
                kSecAttrService as String: service,
                kSecAttrAccount as String: account
            ]

            SecItemDelete(query as CFDictionary)

            guard let data = newValue?.data(using: .utf8) else { return }

            var attributes = query
            attributes[kSecValueData as String] = data
            attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock

            SecItemAdd(attributes as CFDictionary, nil)
        }
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
