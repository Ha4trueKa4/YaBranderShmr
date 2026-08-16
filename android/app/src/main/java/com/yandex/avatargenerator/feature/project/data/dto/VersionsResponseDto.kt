package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ `GET /projects/{id}/versions` — список завёрнут в объект.
 *
 * Поле nullable, как и всё остальное в DTO этой фичи: `required` в контракте
 * не проставлен. Отсутствие `versions` маппер трактует как пустую историю —
 * ронять экран версий из-за этого не за что, пользователю «истории нет» и
 * «поля нет» выглядят одинаково.
 */
@Serializable
data class VersionsResponseDto(
    @SerialName("versions") val versions: List<VersionInfoDto>? = null,
)
