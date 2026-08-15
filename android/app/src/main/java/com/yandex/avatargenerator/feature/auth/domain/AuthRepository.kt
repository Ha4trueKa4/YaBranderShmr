package com.yandex.avatargenerator.feature.auth.domain

import com.yandex.avatargenerator.feature.auth.domain.model.AuthSession

/**
 * Вход, регистрация и владение текущей сессией. Полученный токен реализация
 * сама кладёт в [com.yandex.avatargenerator.core.network.AuthTokenStore].
 */
interface AuthRepository {

    /**
     * `POST /auth/register`. Email реализация нормализует, никнейм обрезает.
     *
     * @throws AuthException.RegistrationRejected поле не прошло проверку (400).
     * @throws AuthException.RegistrationConflict email или никнейм заняты (409).
     */
    suspend fun register(email: String, password: String, nickname: String): AuthSession

    /**
     * `POST /auth/login`.
     *
     * @throws AuthException.InvalidCredentials неверный email или пароль (401).
     */
    suspend fun login(email: String, password: String): AuthSession

    /** Локальный разлогин: ручки на бэкенде нет. */
    suspend fun logout()
}
