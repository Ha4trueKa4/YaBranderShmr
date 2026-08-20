import Foundation

/// Получение профиля пользователя из сети. Выделен в протокол для подмены
/// моковой реализацией в превью и тестах.
///
/// Конвенция изоляции сервисов: stateless-сервисы ввода-вывода (сеть, диск) —
/// `nonisolated ... Sendable`; стейтфул-хранилища с наблюдаемым состоянием —
/// `@MainActor` (см. `CurrentUserService`).
nonisolated protocol UserProfileService: Sendable {
    func profile(for id: UUID) async throws -> UserProfile
}
