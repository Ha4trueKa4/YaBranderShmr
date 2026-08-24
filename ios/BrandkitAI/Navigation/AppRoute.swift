import Foundation

enum ProfileConnectionsKind: Hashable {
    case followers
    case following

    var title: String {
        switch self {
        case .followers: String(localized: "Followers")
        case .following: String(localized: "connections.following.title", defaultValue: "Following")
        }
    }
}

// Карта главного флоу: экраны, открываемые поверх корня раздела.
enum AppRoute: Hashable {
    case projectCreate
    case chat(projectID: UUID, title: String)
    case chatImage(projectID: UUID, title: String, versionID: UUID, createdAt: Date?)
    case projectImage(projectID: UUID, title: String, imageURL: URL?)
    case projectEditor(projectID: UUID)
    case versionHistory(projectID: UUID)
    case publicProject(projectID: UUID)
    case profile(userID: UUID)
    case profileConnections(userID: UUID, kind: ProfileConnectionsKind)
    case settings
}
