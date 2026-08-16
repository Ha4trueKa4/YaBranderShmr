import Foundation

// Карта главного флоу: экраны, открываемые поверх корня раздела.
enum AppRoute: Hashable {
    case projectCreate
    case chat(projectID: UUID, title: String)
    case projectEditor(projectID: UUID)
    case versionHistory(projectID: UUID)
    case publicProject(projectID: UUID)
    case profile(userID: UUID)
}
