import Foundation

enum ProfileConnectionsKind: Hashable {
    case followers
    case following

    /// Заголовок экрана. Ключи семантические: в английском «Following» —
    /// и заголовок списка, и состояние кнопки подписки, а в русском это
    /// разные слова.
    var title: String {
        switch self {
        case .followers:
            String(
                localized: "connections.title.followers",
                defaultValue: "Followers",
                comment: "Navigation title of the followers list"
            )
        case .following:
            String(
                localized: "connections.title.following",
                defaultValue: "Following",
                comment: "Navigation title of the list of accounts the user follows"
            )
        }
    }

    /// Заголовок пустого состояния целиком: в русском нужен родительный падеж,
    /// поэтому подстановкой названия списка обойтись нельзя.
    var emptyTitle: String {
        switch self {
        case .followers: String(localized: "No Followers Yet")
        case .following: String(localized: "No Following Yet")
        }
    }

    var emptyDescription: String {
        switch self {
        case .followers: String(localized: "Followers will appear here.")
        case .following: String(localized: "Followed accounts will appear here.")
        }
    }

    /// Текст ошибки загрузки целиком — по той же причине, что и `emptyTitle`.
    var loadFailureMessage: String {
        switch self {
        case .followers: String(localized: "Couldn't load followers.")
        case .following: String(localized: "Couldn't load following.")
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
