package com.yandex.avatargenerator.core.network

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Токен в памяти процесса. Перезапуск приложения не переживает, поэтому в
 * приложении используется [EncryptedAuthTokenStore] — эта реализация осталась
 * для тестов, где Android-контекста нет.
 */
class InMemoryAuthTokenStore : AuthTokenStore {

    private val state = MutableStateFlow<String?>(null)

    override val token: Flow<String?> = state.asStateFlow()

    override suspend fun currentToken(): String? = state.value

    override suspend fun update(token: String) {
        state.value = token
    }

    override suspend fun clear() {
        state.value = null
    }
}
