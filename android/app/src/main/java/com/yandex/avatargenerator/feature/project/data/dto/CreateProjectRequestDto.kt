package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Тело `POST /projects` — создание проекта вместе с запуском первичной
 * генерации: [prompt] обязателен (1…5000 символов), ответ — `202` и `job_id`.
 */
@Serializable
data class CreateProjectRequestDto(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("prompt") val prompt: String,
)
