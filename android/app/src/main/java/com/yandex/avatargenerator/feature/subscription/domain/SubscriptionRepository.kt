package com.yandex.avatargenerator.feature.subscription.domain

import com.yandex.avatargenerator.feature.subscription.domain.model.SubscriptionUser

/**
 * Подписки пользователя: кнопка Follow (`POST`/`DELETE /users/{id}/subscribe`) и
 * списки подписчиков/подписок (`GET /users/{id}/subscribers`,
 * `GET /users/{id}/subscriptions`). Общий для профиля, публичного проекта и
 * экрана подписок.
 *
 * [subscribe]/[unsubscribe] идемпотентны на бэкенде и отдают [Result]: экран
 * делает оптимистичное обновление и откатывает его при `isFailure`. Списочные
 * методы бросают исключение при сбое (как [com.yandex.avatargenerator.feature.profile.domain.ProfileRepository]) —
 * экран заворачивает вызов в `runCatching` и показывает состояние ошибки.
 */
interface SubscriptionRepository {

    /** Подписаться на пользователя [userId]. */
    suspend fun subscribe(userId: String): Result<Unit>

    /** Отписаться от пользователя [userId]. */
    suspend fun unsubscribe(userId: String): Result<Unit>

    /** Подписчики пользователя [userId] (кто подписан на него). */
    suspend fun getFollowers(userId: String): List<SubscriptionUser>

    /** Подписки пользователя [userId] (на кого подписан он). */
    suspend fun getFollowing(userId: String): List<SubscriptionUser>
}
