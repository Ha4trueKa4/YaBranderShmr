package com.yandex.avatargenerator.feature.auth

import com.yandex.avatargenerator.feature.auth.domain.AuthRepository
import com.yandex.avatargenerator.feature.auth.domain.model.AuthSession
import com.yandex.avatargenerator.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Репозиторий-подделка для тестов ViewModel входа и регистрации.
 *
 * Сети здесь нет: успех и сбой задаются полями, а вызовы записываются, чтобы
 * проверить, что до репозитория дошли именно введённые в форму значения.
 */
internal class FakeAuthRepository : AuthRepository {

    private val _isAuthorized = MutableStateFlow(false)
    override val isAuthorized: Flow<Boolean> = _isAuthorized.asStateFlow()

    /** Текущее значение [isAuthorized] — читать в тесте удобнее, чем собирать Flow. */
    val authorized: Boolean get() = _isAuthorized.value

    /** Если задано — [login] и [register] бросают это вместо успешного ответа. */
    var failure: Exception? = null

    /** Сессия, которую вернуть на успешный вход или регистрацию. */
    var session: AuthSession = AuthSession(
        accessToken = "jwt-token",
        tokenType = "Bearer",
        user = AuthUser(id = "user-1", email = "user@example.com", nickname = "Nick"),
    )

    var loginCallCount: Int = 0
        private set
    var registerCallCount: Int = 0
        private set

    /** Аргументы последнего вызова — для проверки, что они дошли до репозитория. */
    var lastEmail: String? = null
        private set
    var lastPassword: String? = null
        private set
    var lastNickname: String? = null
        private set

    /**
     * Подвесить запрос: [login] и [register] уйдут в ожидание и завершатся только
     * после [resume]. Нужно, чтобы поймать состояние «запрос ещё идёт» —
     * со StandardTestDispatcher `advanceUntilIdle()` вернёт управление тесту,
     * пока корутина висит на этом ожидании.
     */
    var suspendUntilResumed: Boolean = false

    private val gate = CompletableDeferred<Unit>()

    /** Отпускает подвешенный запрос — см. [suspendUntilResumed]. */
    fun resume() {
        gate.complete(Unit)
    }

    override suspend fun register(email: String, password: String, nickname: String): AuthSession {
        registerCallCount++
        lastEmail = email
        lastPassword = password
        lastNickname = nickname
        return awaitResult()
    }

    override suspend fun login(email: String, password: String): AuthSession {
        loginCallCount++
        lastEmail = email
        lastPassword = password
        return awaitResult()
    }

    override suspend fun logout() {
        _isAuthorized.value = false
    }

    /** Общий хвост запроса: подождать (если просили), упасть или отдать сессию. */
    private suspend fun awaitResult(): AuthSession {
        if (suspendUntilResumed) gate.await()
        failure?.let { throw it }
        _isAuthorized.value = true
        return session
    }
}
