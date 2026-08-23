package com.yandex.avatargenerator.feature.create.data.dto

import com.yandex.avatargenerator.core.network.dto.ProjectDescriptionDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ `GET /users/{id}` глазами списка чатов: из профиля нужны только
 * `projects[]`, остальные поля отбрасывает `ignoreUnknownKeys` из `ApiJson`.
 *
 * Полный разбор того же ответа — в
 * [com.yandex.avatargenerator.feature.profile.data.dto.UserProfileResponseDto].
 */
@Serializable
data class UserProjectsResponseDto(
    @SerialName("projects") val projects: List<ProjectDescriptionDto>? = null,
)
