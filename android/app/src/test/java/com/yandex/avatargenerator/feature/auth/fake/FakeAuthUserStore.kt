package com.yandex.avatargenerator.feature.auth.fake

import com.yandex.avatargenerator.feature.auth.data.AuthUserStore
import com.yandex.avatargenerator.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Профиль в памяти — тому же назначению служит `InMemoryAuthTokenStore` для
 * токена: в юнит-тестах Android-контекста и `DataStore` нет.
 */
internal class FakeAuthUserStore : AuthUserStore {

    private val state = MutableStateFlow<AuthUser?>(null)

    override val user: Flow<AuthUser?> = state.asStateFlow()

    /** Текущее значение [user] — читать в тесте удобнее, чем собирать Flow. */
    val savedUser: AuthUser? get() = state.value

    override suspend fun save(user: AuthUser) {
        state.value = user
    }

    override suspend fun clear() {
        state.value = null
    }
}
