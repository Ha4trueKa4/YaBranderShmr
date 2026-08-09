import Foundation

/// Карта главного флоу: экраны, открываемые поверх корня раздела.
/// Такой экран перекрывает таб-бар, потому что бар навешен на корень стека.
///
/// Ассоциированные значения — то, без чего экран не имеет смысла. Часть экранов
/// их пока не использует (данных нет), но маршрут уже переносит их правильно,
/// поэтому подключение моделей не потребует менять enum.
enum AppRoute: Hashable {
    case projectCreate
    /// Чат проекта — он же результат генерации: в макете разговор начинается
    /// с системной плашки «Project created successfully».
    case chat(projectID: UUID, title: String)
    case projectEditor(projectID: UUID)
    case versionHistory(projectID: UUID)
    case publicProject(projectID: UUID)
    case profile(userID: UUID)
}
