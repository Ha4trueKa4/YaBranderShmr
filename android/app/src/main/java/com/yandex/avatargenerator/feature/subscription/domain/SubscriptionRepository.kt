package com.yandex.avatargenerator.feature.subscription.domain

/**
 * Подписка на пользователя (`POST`/`DELETE /users/{id}/subscribe`). Общий для
 * профиля и публичного проекта: кнопка Follow есть на обоих экранах.
 *
 * Обе операции идемпотентны на бэкенде. Наружу отдаём [Result]: экран делает
 * оптимистичное обновление и откатывает его при `isFailure`.
 */
interface SubscriptionRepository {

    /** Подписаться на пользователя [userId]. */
    suspend fun subscribe(userId: String): Result<Unit>

    /** Отписаться от пользователя [userId]. */
    suspend fun unsubscribe(userId: String): Result<Unit>
}
