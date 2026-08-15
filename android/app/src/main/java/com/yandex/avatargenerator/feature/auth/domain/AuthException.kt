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
     * `400` на `POST /auth/register`: значение не прошло проверку сервера.
     * Сами правила на клиенте не дублируем — они живут на бэкенде.
     */
    class RegistrationRejected(
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
}
