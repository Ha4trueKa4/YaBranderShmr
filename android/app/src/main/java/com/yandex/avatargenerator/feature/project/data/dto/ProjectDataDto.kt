package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Содержимое холста (`ProjectData`). Поля nullable: в swagger нет `required`,
 * а значения по умолчанию замаскировали бы отсутствие поля.
 */
@Serializable
data class ProjectDataDto(
    @SerialName("width") val width: Int? = null,
    @SerialName("height") val height: Int? = null,
    /** Цвет фона в hex, например `#FFFFFF`. */
    @SerialName("background") val background: String? = null,
    /** SVG-разметка проекта одной строкой. */
    @SerialName("layers") val layers: String? = null,
)
