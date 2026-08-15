import SwiftUI

@MainActor
@Observable
final class PublicProjectViewModel {
    struct RelatedProject: Identifiable {
        let id: UUID
        let title: String
        let colors: [Color]
    }

    let projectID: UUID
    let title = "Nova Studio Brand"
    let publishedText = "Published 3 days ago"
    let authorName = "Sarah Chen"
    let authorRole = "Product Designer"
    private(set) var isFollowing = false

    let moreProjects: [RelatedProject] = [
        RelatedProject(id: UUID(), title: "Vortex Lab", colors: [.pink, .orange]),
        RelatedProject(id: UUID(), title: "Aero Pattern", colors: [.mint, .cyan]),
        RelatedProject(id: UUID(), title: "Electric Horizon", colors: [.purple, .blue]),
        RelatedProject(id: UUID(), title: "Rose Signal", colors: [.purple, .pink])
    ]

    init(projectID: UUID) {
        self.projectID = projectID
    }

    var authorInitials: String {
        authorName.split(separator: " ").prefix(2).compactMap(\.first).map(String.init).joined()
    }

    func toggleFollowing() {
        isFollowing.toggle()
    }
}
