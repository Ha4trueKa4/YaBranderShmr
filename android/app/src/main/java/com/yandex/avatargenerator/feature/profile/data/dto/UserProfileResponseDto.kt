package com.yandex.avatargenerator.feature.profile.data.dto

import com.yandex.avatargenerator.feature.project.data.dto.ProjectDescriptionDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ `GET /users/{id}` — профиль и его проекты.
 *
 * Имя здесь в `name`, а `POST /auth/login` отдаёт `nickname` — см. `API.md`.
 */
@Serializable
data class UserProfileResponseDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("projects") val projects: List<ProjectDescriptionDto>? = null,
)
