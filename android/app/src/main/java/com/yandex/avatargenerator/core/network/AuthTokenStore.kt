package com.yandex.avatargenerator.core.network

/**
 * Хранилище access-токена: пишет репозиторий авторизации, читает клиент.
 * Клиенту отдаём только [AuthTokenProvider] — по типу зависимости видно,
 * кто владеет сессией.
 */
interface AuthTokenStore : AuthTokenProvider {

    suspend fun update(token: String)

    suspend fun clear()
}
