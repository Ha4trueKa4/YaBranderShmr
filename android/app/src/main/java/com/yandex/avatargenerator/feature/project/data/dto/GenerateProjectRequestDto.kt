package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Тело `POST /projects/{id}/generate` — промпт для ИИ.
 *
 * @property model выбор модели. Не заполняем: бэкенд подставит свой дефолт, а
 *  `explicitNulls = false` в [com.yandex.avatargenerator.core.network.ApiJson]
 *  выкинет поле из JSON. Заведено под будущий выбор модели в UI.
 */
@Serializable
data class GenerateProjectRequestDto(
    @SerialName("prompt") val prompt: String,
    @SerialName("model") val model: String? = null,
)
