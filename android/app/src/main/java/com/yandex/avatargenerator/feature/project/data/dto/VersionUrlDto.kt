package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ `GET /projects/{id}/versions/{versionId}` и
 * `POST /projects/{id}/versions/{versionId}/restore` — форма у них одна.
 *
 * [url] — presigned S3-ссылка: подписана, протухает, поэтому в кэш надолго
 * не годится.
 */
@Serializable
data class VersionUrlDto(
    @SerialName("version_id") val versionId: String? = null,
    @SerialName("url") val url: String? = null,
)
