package com.yandex.avatargenerator.core.network

import androidx.core.net.toUri
import com.yandex.avatargenerator.BuildConfig

/**
 * Параметры OAuth-приложения Yandex ID для web-флоу (`response_type=code`).
 *
 * Клиент только открывает страницу согласия и ловит `code` из редиректа — обмен
 * `code` на токен делает бэкенд (`POST /auth/yandex`), у него же лежит
 * `client_secret`. Значения приходят из `BuildConfig` (см. `app/build.gradle.kts`),
 * фичи `BuildConfig` не читают — берут отсюда, как из [ApiConfig].
 */
object YandexOAuthConfig {

    val ClientId: String = BuildConfig.YANDEX_CLIENT_ID

    /** Обязан совпадать с зарегистрированным в приложении Yandex ID. */
    val RedirectUri: String = BuildConfig.YANDEX_REDIRECT_URI

    /**
     * Задан ли `client_id`. Без него открывать страницу согласия бессмысленно —
     * Yandex ответит «отсутствует обязательный параметр client_id», поэтому в
     * несконфигурированной сборке кнопку входа через Yandex ID вообще не
     * показываем.
     */
    val IsConfigured: Boolean get() = ClientId.isNotBlank()

    private const val AuthorizeEndpoint = "https://oauth.yandex.ru/authorize"

    /**
     * URL страницы согласия — те же параметры, что у веб-клиента
     * (`frontend/.../yandexOAuth.ts`), чтобы флоу совпадал.
     *
     * `scope=login:email` обязателен: без него профиль придёт без email и
     * бэкенд ответит `YANDEX_EMAIL_SCOPE_REQUIRED`. `state` — защита от CSRF:
     * возвращается в редиректе без изменений, экран сверяет его с отправленным.
     * `force_confirm=yes` — всегда показывать экран согласия и выбор аккаунта:
     * WebView держит куки Yandex между входами, и без этого на общем устройстве
     * следующий пользователь молча вошёл бы под сессией предыдущего.
     */
    fun authorizeUrl(state: String): String = AuthorizeEndpoint.toUri().buildUpon()
        .appendQueryParameter("response_type", "code")
        .appendQueryParameter("client_id", ClientId)
        .appendQueryParameter("redirect_uri", RedirectUri)
        .appendQueryParameter("scope", "login:email")
        .appendQueryParameter("state", state)
        .appendQueryParameter("force_confirm", "yes")
        .build()
        .toString()
}
