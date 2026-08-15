package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Тело `PATCH /projects/{id}`: `null` означает «не трогать». Работает за счёт
 * `explicitNulls = false` в [com.yandex.avatargenerator.core.network.ApiJson] —
 * null-поля не попадают в JSON.
 */
@Serializable
data class UpdateProjectRequestDto(
    @SerialName("is_public") val isPublic: Boolean? = null,
    /** SVG-разметка проекта одной строкой (ручное сохранение холста). */
    @SerialName("data") val data: String? = null,
)
