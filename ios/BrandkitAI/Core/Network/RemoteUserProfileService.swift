import Foundation

nonisolated final class RemoteUserProfileService: UserProfileService {
    private let networkClient: NetworkClientProtocol

    init(networkClient: NetworkClientProtocol = NetworkClient.shared) {
        self.networkClient = networkClient
    }

    func profile(for id: UUID) async throws -> UserProfile {
        let dto: UserProfileDTO = try await networkClient.request(
            .get,
            .userProfile(id: id),
            authorization: .optional
        )
        return UserProfile(
            id: dto.id,
            name: dto.name,
            subscribersCount: dto.subscribersCount ?? 0,
            subscriptionsCount: dto.subscriptionsCount ?? 0,
            isSubscribed: dto.isSubscribed,
            projects: (dto.projects ?? []).map {
                UserProject(id: $0.id, name: $0.name, url: $0.url)
            }
        )
    }
}

private nonisolated struct UserProfileDTO: Decodable, Sendable {
    let id: UUID
    let name: String
    let subscribersCount: Int?
    let subscriptionsCount: Int?
    let isSubscribed: Bool?
    let projects: [UserProjectDTO]?
}

private nonisolated struct UserProjectDTO: Decodable, Sendable {
    let id: UUID
    let name: String
    let url: URL?
}
