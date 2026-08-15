package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Тело `POST /projects/{id}/generate` — промпт для ИИ. */
@Serializable
data class GenerateProjectRequestDto(
    @SerialName("prompt") val prompt: String,
)
