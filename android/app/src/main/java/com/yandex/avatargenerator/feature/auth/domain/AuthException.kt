package com.yandex.avatargenerator.feature.auth.domain

/**
 * Ошибки входа и регистрации, которые экран показывает по-своему. Всё
 * остальное остаётся [com.yandex.avatargenerator.core.domain.NetworkException].
 */
sealed class AuthException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** Поле формы, на которое вешать ошибку. `Unknown` — код нам ещё незнаком. */
    enum class Field { Email, Password, Nickname, Unknown }

    /** `401` на `POST /auth/login`. */
    class InvalidCredentials(cause: Throwable? = null) :
        AuthException("Invalid email or password", cause)

    /**
     * `400`/`422` на входе или регистрации: значение поля не прошло проверку
     * сервера. Сами правила на клиенте не дублируем — они живут на бэкенде.
     */
    class FieldRejected(
        val field: Field,
        val serverMessage: String? = null,
        cause: Throwable? = null,
    ) : AuthException(serverMessage, cause)

    /** `409` на `POST /auth/register`: email или никнейм заняты. */
    class RegistrationConflict(
        val field: Field,
        val serverMessage: String? = null,
        cause: Throwable? = null,
    ) : AuthException(serverMessage, cause)

    /**
     * `409` на `POST /auth/yandex`: email из профиля Yandex уже принадлежит
     * аккаунту с паролем. Автопривязки нет намеренно — нужно войти по паролю
     * и привязать Yandex ID отдельно.
     */
    class YandexEmailConflict(cause: Throwable? = null) :
        AuthException("Email already registered with a password", cause)

    /**
     * `400`/`502` на `POST /auth/yandex`: Yandex ID недоступен, `code` протух,
     * либо приложение не запросило скоуп `login:email`. Пользователю — общее
     * «не удалось, попробуйте ещё раз».
     */
    class YandexAuthFailed(cause: Throwable? = null) :
        AuthException("Yandex ID authentication failed", cause)
}
