package com.yandex.avatargenerator.core.network

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Токен в памяти процесса. Перезапуск приложения не переживает — когда
 * появится DataStore, меняем реализацию в
 * [com.yandex.avatargenerator.core.di.networkModule].
 */
class InMemoryAuthTokenStore : AuthTokenStore {

    private val token = MutableStateFlow<String?>(null)

    override suspend fun currentToken(): String? = token.value

    override suspend fun update(token: String) {
        this.token.value = token
    }

    override suspend fun clear() {
        token.value = null
    }
}
