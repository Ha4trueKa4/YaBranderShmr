package com.yandex.avatargenerator.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Метаданные проекта без холста (`ProjectDescription`), списком в `GET /users/{id}`.
 *
 * Общий кусок ответов `GET /users/{id}` и `GET /projects/{id}` — владельца среди
 * фич у него нет, поэтому живёт в `core`.
 */
@Serializable
data class ProjectDescriptionDto(
    @SerialName("id") val id: String? = null,
    @SerialName("author_id") val authorId: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    /** ISO 8601 date-time строкой; парсинг в дату — на стороне маппера. */
    @SerialName("created_at") val createdAt: String? = null,
    /** S3-ссылка на превью проекта (SVG); отсутствует у пустого проекта. */
    @SerialName("url") val url: String? = null,
)
