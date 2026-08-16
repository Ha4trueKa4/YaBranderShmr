import Foundation

/// Данные авторизованного пользователя, которые нужны приложению поверх токена.
///
/// Отвечает только за id и имя текущего пользователя; полный профиль получает
/// `UserProfileService`. Выделен в протокол, чтобы экраны и тесты могли получать
/// моковую реализацию.
@MainActor
protocol CurrentUserService: AnyObject {
    var id: String? { get }
    var name: String? { get }

    /// Восстанавливает id/name из хранилища, а при их отсутствии — из JWT и API.
    func restore() async

    func save(id: String, name: String?)
    func clear()
}
