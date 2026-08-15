package com.yandex.avatargenerator.core.network

import kotlinx.coroutines.flow.Flow

/**
 * Хранилище access-токена: пишет репозиторий авторизации, читает клиент.
 * Клиенту отдаём только [AuthTokenProvider] — по типу зависимости видно,
 * кто владеет сессией.
 */
interface AuthTokenStore : AuthTokenProvider {

    /**
     * Текущий токен и все его изменения. Нужен навигации, чтобы реактивно
     * узнать про наличие сессии. Первое значение приходит после чтения
     * хранилища, поэтому подписчик не увидит ложного «токена нет» на старте.
     */
    val token: Flow<String?>

    suspend fun update(token: String)

    suspend fun clear()
}
