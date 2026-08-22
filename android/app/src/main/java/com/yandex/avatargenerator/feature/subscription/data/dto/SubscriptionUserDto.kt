package com.yandex.avatargenerator.feature.subscription.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Элемент ответа `GET /users/{id}/subscribers` и `/subscriptions`.
 *
 * Ручек ещё нет в бэкенде (см. `API.md`), поэтому набор полей — по аналогии с
 * `GET /users/{id}` (`UserProfileResponseDto`): имя в `name`, snake_case. Всё
 * опционально + `ignoreUnknownKeys`, чтобы не падать на расхождениях контракта.
 */
@Serializable
data class SubscriptionUserDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("subscribers_count") val subscribersCount: Int? = null,
    @SerialName("is_subscribed") val isSubscribed: Boolean? = null,
)
