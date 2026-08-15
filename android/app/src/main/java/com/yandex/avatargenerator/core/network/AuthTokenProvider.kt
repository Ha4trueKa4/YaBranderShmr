package com.yandex.avatargenerator.core.network

/**
 * Источник access-токена для заголовка `Authorization`.
 *
 * Нужен, чтобы [HttpClientFactory] не зависел от фичи `auth`. Запись — в
 * [AuthTokenStore].
 */
fun interface AuthTokenProvider {

    /** Текущий токен или `null`, если пользователь не авторизован. */
    suspend fun currentToken(): String?
}
