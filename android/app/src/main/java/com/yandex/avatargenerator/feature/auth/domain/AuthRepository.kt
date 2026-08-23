package com.yandex.avatargenerator.feature.auth.domain

import com.yandex.avatargenerator.feature.auth.domain.model.AuthSession
import com.yandex.avatargenerator.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

/**
 * Вход, регистрация и владение текущей сессией. Полученный токен реализация
 * сама кладёт в [com.yandex.avatargenerator.core.network.AuthTokenStore].
 */
interface AuthRepository {

    /**
     * Есть ли сохранённая сессия. Первое значение приходит после чтения
     * хранилища, поэтому подписчик не увидит ложного «не авторизован» на старте.
     *
     * Токен наружу не отдаём: `domain/` не должен знать про `core/network`.
     */
    val isAuthorized: Flow<Boolean>

    /**
     * Текущий пользователь или `null`. Источник — ответ входа/регистрации: ручки
     * `/auth/me` в API нет, поэтому другого способа узнать свой id у клиента нет.
     *
     * `null` возможен и при действующей сессии — если вход был до появления
     * хранилища профиля (см. `API.md`).
     */
    val currentUser: Flow<AuthUser?>

    /**
     * `POST /auth/register`. Email реализация нормализует, никнейм обрезает.
     *
     * @throws AuthException.FieldRejected поле не прошло проверку (400 или 422).
     * @throws AuthException.RegistrationConflict email или никнейм заняты (409).
     */
    suspend fun register(email: String, password: String, nickname: String): AuthSession

    /**
     * `POST /auth/login`.
     *
     * @throws AuthException.InvalidCredentials неверный email или пароль (401).
     * @throws AuthException.FieldRejected сервер отверг формат поля (400 или 422).
     */
    suspend fun login(email: String, password: String): AuthSession

    /**
     * `POST /auth/yandex` (web-флоу). `code` из редиректа страницы согласия
     * бэкенд меняет на токен и заводит аккаунт, если такого ещё не было.
     *
     * @throws AuthException.YandexEmailConflict email занят аккаунтом с паролем (409).
     * @throws AuthException.YandexAuthFailed Yandex ID не отдал профиль (400/502).
     */
    suspend fun loginWithYandex(code: String): AuthSession

    /** Локальный разлогин: ручки на бэкенде нет. */
    suspend fun logout()
}
