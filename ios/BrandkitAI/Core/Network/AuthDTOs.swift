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
