package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Элемент списка версий (`GET /projects/{id}/versions`). Только метаданные:
 * ссылки на SVG в списке нет, за ней — отдельный запрос на версию.
 *
 * Все поля nullable: `required` в контракте не проставлен, а нехватку
 * обязательных полей ловит маппер — иначе `kotlinx.serialization` бросил бы
 * своё исключение раньше, чем мы дали бы ему осмысленное имя.
 */
@Serializable
data class VersionInfoDto(
    @SerialName("id") val id: String? = null,
    @SerialName("version_num") val versionNum: Int? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
