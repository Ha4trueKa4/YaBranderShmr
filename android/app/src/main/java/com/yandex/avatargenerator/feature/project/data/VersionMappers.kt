package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.core.data.toInstantOrNull
import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.feature.project.data.dto.VersionInfoDto
import com.yandex.avatargenerator.feature.project.data.dto.VersionUrlDto
import com.yandex.avatargenerator.feature.project.domain.model.ProjectVersion
import com.yandex.avatargenerator.feature.project.domain.model.VersionRestore

/** DTO выше `data/` не поднимаются: маппинг живёт здесь. */
internal fun VersionInfoDto.toDomain(): ProjectVersion = ProjectVersion(
    id = id.orMalformed("id"),
    number = versionNum ?: malformed("version_num"),
    createdAt = createdAt?.toInstantOrNull(),
)

internal fun VersionUrlDto.toRestore(): VersionRestore = VersionRestore(
    id = versionId.orMalformed("version_id"),
    url = url.orMalformed("url"),
)

/**
 * Ссылка на версию. Ответ без `url` — это сломанный контракт, а не «версия без
 * картинки»: у версии всегда есть объект в S3, ради ссылки на него запрос и
 * делается.
 */
internal fun VersionUrlDto.toUrl(): String = url.orMalformed("url")

/** Пустое обязательное поле — это сломанный контракт, а не «нет значения». */
private fun String?.orMalformed(field: String): String = this ?: malformed(field)

private fun malformed(field: String): Nothing = throw NetworkException.Serialization(
    IllegalStateException("ProjectVersion.$field is missing in response"),
)
