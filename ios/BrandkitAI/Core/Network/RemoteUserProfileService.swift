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

    func subscribe(to userID: UUID) async throws {
        try await networkClient.request(.post, .subscribe(userID: userID), authorization: .required)
    }

    func unsubscribe(from userID: UUID) async throws {
        try await networkClient.request(.delete, .subscribe(userID: userID), authorization: .required)
    }

    func updateName(_ name: String, for id: UUID) async throws -> String {
        let dto: UpdateUserProfileDTO = try await networkClient.request(
            .patch,
            .userProfile(id: id),
            authorization: .required,
            body: UpdateUserProfileRequest(nickname: name)
        )
        return dto.nickname
    }
}

private nonisolated struct UpdateUserProfileRequest: Encodable, Sendable {
    let nickname: String
}

private nonisolated struct UpdateUserProfileDTO: Decodable, Sendable {
    let id: UUID
    let nickname: String
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
