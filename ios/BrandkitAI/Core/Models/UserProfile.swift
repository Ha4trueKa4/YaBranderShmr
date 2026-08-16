import Foundation

/// Профиль пользователя, который отдаёт бэкенд по `GET /users/{id}`.
nonisolated struct UserProfile: Sendable, Equatable {
    let id: UUID
    let name: String
    let subscribersCount: Int
    let subscriptionsCount: Int
    let isSubscribed: Bool?
    let projects: [UserProject]
}
