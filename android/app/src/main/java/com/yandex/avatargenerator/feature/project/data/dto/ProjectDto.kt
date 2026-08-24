package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Проект целиком: ответ `POST /projects` и `GET /projects/{id}`.
 * В swagger это `allOf` (`ProjectDescription` + `data`), но наследования у
 * `@Serializable` нет — поля плоско.
 */
@Serializable
data class ProjectDto(
    @SerialName("id") val id: String? = null,
    @SerialName("author_id") val authorId: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    /** S3-ссылка на объект проекта; отсутствует, если проект пустой. */
    @SerialName("url") val url: String? = null,
    /** SVG-разметка проекта одной строкой; отсутствует у пустого проекта. */
    @SerialName("data") val data: String? = null,
    /** Только в ответе `POST /projects`: id фоновой задачи первичной генерации. */
    @SerialName("job_id") val jobId: String? = null,
)
