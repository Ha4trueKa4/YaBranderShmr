package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Тело `POST /projects` — создание проекта. */
@Serializable
data class CreateProjectRequestDto(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
)
