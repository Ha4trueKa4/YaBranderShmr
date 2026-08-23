import Foundation

nonisolated enum YandexOAuthConfig {
    /// Client ID приложения, зарегистрированного на oauth.yandex.ru.
    ///
    /// ВАЖНО: при смене Client ID синхронно поменять URL-схему `yx<clientID>`
    /// в Config/Info.plist (CFBundleURLTypes): SDK проверяет их совпадение
    /// при активации.
    static let clientID = "34ecb56087424456a5f045cb8ed09701"
}
