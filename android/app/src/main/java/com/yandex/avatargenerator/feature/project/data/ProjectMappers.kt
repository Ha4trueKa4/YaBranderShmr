package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.feature.project.data.dto.ProjectDto
import com.yandex.avatargenerator.feature.project.domain.model.Project
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/** DTO выше `data/` не поднимаются: маппинг живёт здесь. */
internal fun ProjectDto.toDomain(): Project = Project(
    id = id.orMalformed("id"),
    authorId = authorId,
    name = name.orMalformed("name"),
    description = description,
    isPublic = isPublic ?: false,
    createdAt = createdAt?.toInstantOrNull(),
    url = url,
    data = data,
)

/** Пустое обязательное поле — это сломанный контракт, а не «нет значения». */
private fun String?.orMalformed(field: String): String =
    this ?: throw NetworkException.Serialization(
        IllegalStateException("Project.$field is missing in response"),
    )

/**
 * Разбирает `created_at`. `Instant.parse` берёт формы с `Z` и с офсетом
 * (`+03:00`); дату без зоны (`2026-08-14T10:00:00`) он не принимает — её
 * трактуем как UTC (бэкенд хранит время в UTC). Совсем кривой формат не роняет
 * разбор — вернём `null`.
 */
private fun String.toInstantOrNull(): Instant? =
    runCatching { Instant.parse(this) }
        .recoverCatching { LocalDateTime.parse(this).toInstant(ZoneOffset.UTC) }
        .getOrNull()
