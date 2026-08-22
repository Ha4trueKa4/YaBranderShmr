import Foundation
import Security

nonisolated struct KeychainItem: Sendable {
    private let service = "ai.brandkit.auth"
    private let account: String

    init(account: String) {
        self.account = account
    }

    var value: String? {
        get {
            var query = baseQuery
            query[kSecReturnData as String] = true
            query[kSecMatchLimit as String] = kSecMatchLimitOne

            var item: CFTypeRef?
            guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
                  let data = item as? Data
            else {
                return nil
            }

            return String(data: data, encoding: .utf8)
        }

        nonmutating set {
            SecItemDelete(baseQuery as CFDictionary)

            guard let data = newValue?.data(using: .utf8) else { return }

            var attributes = baseQuery
            attributes[kSecValueData as String] = data
            attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock

            SecItemAdd(attributes as CFDictionary, nil)
        }
    }

    private var baseQuery: [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
    }
}

extension KeychainItem {
    nonisolated static let accessToken = KeychainItem(account: "accessToken")
    /// Email не отдаёт ни JWT, ни `GET /users/{id}` — он приходит только в
    /// ответе авторизации. Поэтому лежит рядом с токеном: раз переустановка
    /// сохраняет сессию, она должна сохранять и email.
    nonisolated static let userEmail = KeychainItem(account: "userEmail")
}
