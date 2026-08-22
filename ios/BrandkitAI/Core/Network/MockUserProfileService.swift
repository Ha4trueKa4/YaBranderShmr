import Foundation

#if DEBUG
/// Мок сервиса профилей для превью и тестов.
nonisolated struct MockUserProfileService: UserProfileService {
    let profile: UserProfile

    init(profile: UserProfile = MockUserProfileService.makeSample()) {
        self.profile = profile
    }

    func profile(for id: UUID) async throws -> UserProfile {
        profile
    }

    func subscribe(to userID: UUID) async throws {}
    func unsubscribe(from userID: UUID) async throws {}

    func updateName(_ name: String, for id: UUID) async throws -> String {
        name
    }

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
#endif
