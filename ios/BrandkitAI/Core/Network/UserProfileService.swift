import Foundation

/// Профиль пользователя, который отдаёт бэкенд по `GET /users/{id}`.
nonisolated struct UserProfile: Sendable, Equatable {
    let id: String
    let name: String
    let subscribersCount: Int
    let subscriptionsCount: Int
    let isSubscribed: Bool?
    let projects: [UserProject]
}

nonisolated struct UserProject: Sendable, Equatable, Identifiable {
    let id: String
    let name: String
    let url: String?
}

/// Получение профиля пользователя из сети. Выделен в протокол для подмены
/// моковой реализацией в превью и тестах.
nonisolated protocol UserProfileService: Sendable {
    func profile(for id: String) async throws -> UserProfile
}
