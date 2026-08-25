import Foundation

/// Мок сервиса профилей для превью и тестов.
nonisolated struct MockUserProfileService: UserProfileService {
    let profile: UserProfile

    init(profile: UserProfile = MockUserProfileService.makeSample()) {
        self.profile = profile
    }

    func profile(for id: UUID) async throws -> UserProfile {
        profile
    }

    func subscribers(for userID: UUID) async throws -> [ProfileConnection] {
        Self.sampleConnections
    }

    func subscriptions(for userID: UUID) async throws -> [ProfileConnection] {
        Array(Self.sampleConnections.reversed())
    }

    func subscribe(to userID: UUID) async throws {}
    func unsubscribe(from userID: UUID) async throws {}

    func updateName(_ name: String, for id: UUID) async throws -> String {
        name
    }

    private static let sampleConnections = [
        ProfileConnection(id: UUID(), name: "Ivan Petrov", isSubscribed: false),
        ProfileConnection(id: UUID(), name: "Petr Ivanov", isSubscribed: true),
        ProfileConnection(id: UUID(), name: "Ivanova Ann", isSubscribed: false)
    ]

    private static func makeSample() -> UserProfile {
        UserProfile(
            id: UUID(uuidString: "123e4567-e89b-12d3-a456-426614174000") ?? UUID(),
            name: "Alex Morgan",
            subscribersCount: 340,
            subscriptionsCount: 28,
            isSubscribed: nil,
            projects: [
                UserProject(id: UUID(), name: "Orion Labs", url: nil),
                UserProject(id: UUID(), name: "Cyberpunk Identity", url: nil),
                UserProject(id: UUID(), name: "Aether Brand", url: nil)
            ]
        )
    }
}
