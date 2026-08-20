import SwiftUI

@MainActor
@Observable
final class ProfileViewModel {
    struct Project: Identifiable {
        let id: UUID
        let title: String
        let colors: [Color]
        let coverHeight: CGFloat
    }

    private(set) var name = ""
    private(set) var projectsCount = 0
    private(set) var followersCount = 0
    private(set) var followingCount = 0
    private(set) var projects: [Project] = []
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    private let currentUserService: any CurrentUserService
    private let userProfileService: any UserProfileService
    private let userID: UUID?

    init(
        currentUserService: any CurrentUserService,
        userProfileService: any UserProfileService,
        userID: UUID? = nil
    ) {
        self.currentUserService = currentUserService
        self.userProfileService = userProfileService
        self.userID = userID
        self.name = (userID == nil) ? (currentUserService.name ?? "") : ""
    }

    var initials: String {
        name
            .split(separator: " ")
            .prefix(2)
            .compactMap(\.first)
            .map(String.init)
            .joined()
    }

    func load() async {
        guard !isLoading else { return }
        defer { isLoading = false }

        isLoading = true
        errorMessage = nil

        if userID == nil && currentUserService.id == nil {
            await currentUserService.restore()
        }

        guard let id = userID ?? currentUserService.id else { return }

        do {
            let profile = try await userProfileService.profile(for: id)
            name = profile.name
            followersCount = profile.subscribersCount
            followingCount = profile.subscriptionsCount
            projectsCount = profile.projects.count
            projects = profile.projects.enumerated().map { offset, project in
                Project(
                    id: project.id,
                    title: project.name,
                    colors: Self.palette(for: project.name),
                    coverHeight: Self.coverHeight(for: offset)
                )
            }
        } catch {
            errorMessage = "Couldn't load profile."
        }
    }

    private static let palettes: [[Color]] = [
        [.purple, .indigo],
        [.pink, Color(red: 0.45, green: 0.02, blue: 0.22)],
        [.cyan, Color(red: 0.02, green: 0.12, blue: 0.20)],
        [.orange, Color(red: 0.35, green: 0.13, blue: 0.01)],
        [.blue, .purple],
        [.yellow, .orange],
        [.mint, .blue],
        [.red, .purple]
    ]

    private static let coverHeights: [CGFloat] = [136, 164, 124, 112, 172, 138, 118, 158]

    private static func palette(for title: String) -> [Color] {
        let seed = title.unicodeScalars.reduce(0) { ($0 &+ Int($1.value)) % palettes.count }
        return palettes[seed]
    }

    private static func coverHeight(for offset: Int) -> CGFloat {
        coverHeights[offset % coverHeights.count]
    }
}
