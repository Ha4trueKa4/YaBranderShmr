import Foundation

/// Требование к авторизации запроса: `optional` подставляет токен, если он
/// есть; `required` падает с ошибкой без токена; `none` не шлёт его вовсе.
enum AuthorizationRequirement: Sendable {
    case none
    case optional
    case required
}
