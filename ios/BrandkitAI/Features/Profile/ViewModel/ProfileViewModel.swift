import SwiftUI

@MainActor
@Observable
final class ProfileViewModel {
    struct Project: Identifiable {
        let id = UUID()
        let title: String
        let colors: [Color]
        let coverHeight: CGFloat
    }

    let name = "Alex Morgan"
    let nickname = "@alexmorgan"
    let bio = "Brand designer & AI enthusiast. Creating visual identities."
    let projectsCount = 12
    let followersCount = 340
    let followingCount = 28

    private(set) var projects: [Project] = [
        Project(title: "Orion Labs", colors: [.purple, .indigo], coverHeight: 136),
        Project(title: "Cyberpunk Identity", colors: [.pink, Color(red: 0.45, green: 0.02, blue: 0.22)], coverHeight: 164),
        Project(title: "Aether Brand", colors: [.cyan, Color(red: 0.02, green: 0.12, blue: 0.20)], coverHeight: 124),
        Project(title: "Cosmic Logo", colors: [.orange, Color(red: 0.35, green: 0.13, blue: 0.01)], coverHeight: 112),
        Project(title: "Neon Studio", colors: [.blue, .purple], coverHeight: 172),
        Project(title: "Solar Motion", colors: [.yellow, .orange], coverHeight: 138),
        Project(title: "Nova Systems", colors: [.mint, .blue], coverHeight: 118),
        Project(title: "Velvet Type", colors: [.red, .purple], coverHeight: 158),
        Project(title: "Lumen Brand", colors: [.teal, .indigo], coverHeight: 146),
        Project(title: "Future Shapes", colors: [.pink, .orange], coverHeight: 126)
    ]

    var leftProjects: [Project] {
        projects.enumerated().compactMap { $0.offset.isMultiple(of: 2) ? $0.element : nil }
    }

    var rightProjects: [Project] {
        projects.enumerated().compactMap { $0.offset.isMultiple(of: 2) ? nil : $0.element }
    }

    var initials: String {
        name
            .split(separator: " ")
            .prefix(2)
            .compactMap(\.first)
            .map(String.init)
            .joined()
    }
}
