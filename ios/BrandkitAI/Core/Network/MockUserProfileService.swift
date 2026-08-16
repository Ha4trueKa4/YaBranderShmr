import Foundation

/// Мок сервиса профилей для превью и тестов.
nonisolated struct MockUserProfileService: UserProfileService {
    let profile: UserProfile

    init(profile: UserProfile = MockUserProfileService.makeSample()) {
        self.profile = profile
    }

    func profile(for id: String) async throws -> UserProfile {
        profile
    }

    private static func makeSample() -> UserProfile {
        UserProfile(
            id: "123e4567-e89b-12d3-a456-426614174000",
            name: "Alex Morgan",
            subscribersCount: 340,
            subscriptionsCount: 28,
            isSubscribed: nil,
            projects: [
                UserProject(id: "p1", name: "Orion Labs", url: nil),
                UserProject(id: "p2", name: "Cyberpunk Identity", url: nil),
                UserProject(id: "p3", name: "Aether Brand", url: nil)
            ]
        )
    }
}
