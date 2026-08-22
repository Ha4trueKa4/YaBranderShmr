package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.feature.project.domain.VersionsRepository
import com.yandex.avatargenerator.feature.project.domain.model.ProjectVersion
import com.yandex.avatargenerator.feature.project.domain.model.VersionRestore

/**
 * Репозиторий версий-подделка для тестов ViewModel: нужен ради превью прошлых
 * версий — их ссылки экран догружает по одной.
 *
 * @param urls готовые ссылки по versionId; версии не из этой карты отвечают
 *  ошибкой — так проверяется, что упавшая догрузка не ломает ленту.
 */
internal class FakeVersionsRepository(
    val urls: MutableMap<String, String> = mutableMapOf(),
) : VersionsRepository {

    /** Версии, за ссылками на которые ходили: по ним видно лишние запросы. */
    val requestedVersionIds: MutableList<String> = mutableListOf()

    override suspend fun getVersions(projectId: String): Result<List<ProjectVersion>> =
        Result.success(emptyList())

    override suspend fun getVersionUrl(projectId: String, versionId: String): Result<String> {
        requestedVersionIds += versionId
        return urls[versionId]?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("no url for $versionId"))
    }

    override suspend fun restoreVersion(
        projectId: String,
        versionId: String,
    ): Result<VersionRestore> = Result.failure(UnsupportedOperationException())
}
