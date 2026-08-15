import Foundation

/// Получение профиля пользователя с бэкенда через `NetworkClient`.
nonisolated final class RemoteUserProfileService: UserProfileService {
    private let networkClient: NetworkClient

    init(networkClient: NetworkClient) {
        self.networkClient = networkClient
    }

    func profile(for id: String) async throws -> UserProfile {
        let response: UserProfileResponse = try await networkClient.request(
            .get,
            path: "/users/\(id)",
            authorization: .optional
        )
        return UserProfile(
            id: response.id,
            name: response.name,
            subscribersCount: response.subscribersCount ?? 0,
            subscriptionsCount: response.subscriptionsCount ?? 0,
            isSubscribed: response.isSubscribed,
            projects: (response.projects ?? []).map {
                UserProject(id: $0.id, name: $0.name, url: $0.url)
            }
        )
    }
}

private nonisolated struct UserProfileResponse: Decodable, Sendable {
    let id: String
    let name: String
    let subscribersCount: Int?
    let subscriptionsCount: Int?
    let isSubscribed: Bool?
    let projects: [UserProjectResponse]?

    enum CodingKeys: String, CodingKey {
        case id
        case name
        case subscribersCount = "subscribers_count"
        case subscriptionsCount = "subscriptions_count"
        case isSubscribed = "is_subscribed"
        case projects
    }
}

private nonisolated struct UserProjectResponse: Decodable, Sendable {
    let id: String
    let name: String
    let url: String?
}
