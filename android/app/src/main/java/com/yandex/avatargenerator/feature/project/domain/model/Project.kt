package com.yandex.avatargenerator.feature.project.domain.model

import java.time.Instant

/**
 * Проект целиком (`GET /projects/{id}`).
 *
 * `id` и `name` не-nullable: проект без них — это сломанный контракт, а не
 * «пустое» значение, поэтому маппер в таком случае бросает
 * [com.yandex.avatargenerator.core.domain.NetworkException.Serialization].
 * Остальные поля по контракту опциональны и остаются nullable.
 */
data class Project(
    val id: String,
    val authorId: String?,
    val name: String,
    val description: String?,
    /** Видимость: приватный по умолчанию (см. отсутствие `is_public` в ответе). */
    val isPublic: Boolean,
    val createdAt: Instant?,
    /** S3-ссылка на объект проекта; null, если проект пустой. */
    val url: String?,
    /** SVG-разметка проекта строкой; null, если проект пустой. */
    val data: String?,
)
