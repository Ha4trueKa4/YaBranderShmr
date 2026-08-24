package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Тело `POST /projects/{id}/generate` — промпт для ИИ.
 *
 * @property model короткое имя выбранной модели — **без** префикса `gpt://…`,
 *  бэкенд клеит его сам. `null` (модель не выбирали) не уедет в теле вовсе:
 *  `explicitNulls = false` в [com.yandex.avatargenerator.core.network.ApiJson]
 *  выкинет поле из JSON, и модель подставит бэкенд.
 */
@Serializable
data class GenerateProjectRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("model") val model: String? = null,
)
