import Foundation

/// Карта главного флоу: экраны, открываемые поверх корня раздела.
/// Такой экран перекрывает таб-бар, потому что бар навешен на корень стека.
///
/// Ассоциированные значения — то, без чего экран не имеет смысла. Экраны их
/// пока не используют (данных нет), но маршрут уже переносит их правильно,
/// поэтому подключение моделей не потребует менять enum.
enum AppRoute: Hashable {
    case projectCreate
    case projectGenerating(projectID: UUID)
    case projectEditor(projectID: UUID)
    case versionHistory(projectID: UUID)
    case publicProject(projectID: UUID)
    case profile(userID: UUID)
}
