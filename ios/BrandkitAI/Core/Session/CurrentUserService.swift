import Foundation

/// Данные авторизованного пользователя, которые нужны приложению поверх токена.
///
/// Отвечает только за id и имя текущего пользователя; полный профиль получает
/// `UserProfileService`. Выделен в протокол, чтобы экраны и тесты могли получать
/// моковую реализацию.
///
/// Конвенция изоляции сервисов: стейтфул-хранилища с наблюдаемым состоянием —
/// `@MainActor`; stateless-сервисы ввода-вывода — `nonisolated ... Sendable`
/// (см. `UserProfileService`).
@MainActor
protocol CurrentUserService: AnyObject {
    var id: UUID? { get }
    var name: String? { get }

    /// Восстанавливает id/name из хранилища, а при их отсутствии — из JWT и API.
    func restore() async

    func save(id: UUID, name: String?)
    func clear()
}
