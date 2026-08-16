package com.yandex.avatargenerator.feature.profile.data.dto

import com.yandex.avatargenerator.feature.project.data.dto.ProjectDescriptionDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ `GET /users/{id}` — профиль и его проекты одним запросом.
 *
 * Имя здесь в `name`, а `POST /auth/login` отдаёт `nickname` — см. `API.md`.
 * Полей `bio`/`avatar`/отдельного `handle` в контракте нет: домен подставляет
 * пустые значения (маппинг — в `ProfileMappers.kt`). `is_subscribed`
 * отсутствует при запросе собственного профиля — для чужого он и нужен.
 */
@Serializable
data class UserProfileResponseDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("subscribers_count") val subscribersCount: Int? = null,
    @SerialName("subscriptions_count") val subscriptionsCount: Int? = null,
    @SerialName("is_subscribed") val isSubscribed: Boolean? = null,
    @SerialName("projects") val projects: List<ProjectDescriptionDto>? = null,
)
