import Foundation

// MARK: - DTO (контракт /auth/*)

nonisolated struct LoginRequest: Encodable, Sendable {
    let email: String
    let password: String
}

nonisolated struct RegisterRequest: Encodable, Sendable {
    let email: String
    let password: String
    let nickname: String
}

/// SDK-флоу Yandex ID: клиент шлёт готовый OAuth-токен, бэкенд проверяет его
/// через login.yandex.ru/info?format=jwt (подпись — client_secret).
nonisolated struct YandexAuthRequest: Encodable, Sendable {
    let accessToken: String
}

nonisolated struct AuthResponse: Decodable, Sendable {
    let accessToken: String
    let tokenType: String
    let user: AuthUser
}

nonisolated struct AuthUser: Decodable, Sendable {
    let id: UUID
    let email: String
    let nickname: String
}
