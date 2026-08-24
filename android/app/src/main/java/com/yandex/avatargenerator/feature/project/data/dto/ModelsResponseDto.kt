package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Ответ `GET /models`: `{"object": "list", "data": [...]}`. */
@Serializable
data class ModelsResponseDto(
    @SerialName("object") val objectType: String? = null,
    @SerialName("data") val data: List<ModelDto>? = null,
)
