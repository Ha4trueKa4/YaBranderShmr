package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.feature.project.data.dto.ProjectDto
import com.yandex.avatargenerator.feature.project.domain.model.Project

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
