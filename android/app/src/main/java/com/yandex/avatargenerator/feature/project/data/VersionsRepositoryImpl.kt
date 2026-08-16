package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.core.network.apiResult
import com.yandex.avatargenerator.feature.project.data.dto.VersionUrlDto
import com.yandex.avatargenerator.feature.project.data.dto.VersionsResponseDto
import com.yandex.avatargenerator.feature.project.domain.VersionsRepository
import com.yandex.avatargenerator.feature.project.domain.model.ProjectVersion
import com.yandex.avatargenerator.feature.project.domain.model.VersionRestore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.encodeURLPathPart

private const val ProjectsPath = "projects"
private const val VersionsSegment = "versions"
private const val RestoreSegment = "restore"

/**
 * Тело `POST /restore`. Полей у запроса нет — всё берётся из пути, — но
 * хендлер на бэкенде наследует `HttpHandlerJsonBase` и разбирает тело как JSON
 * ещё до своей логики. Пустое тело он бы разбирать не стал, поэтому шлём
 * пустой объект: DTO без полей ради этого заводить не за что.
 */
private val EmptyJsonBody = TextContent("{}", ContentType.Application.Json)

/**
 * История версий проекта через API. Инъектит готовый [HttpClient] (свой не
 * собирает), каждый вызов заворачивает в [apiCall], `Bearer` подставляет клиент
 * сам. Наружу отдаёт [Result]: ошибку разбирает ViewModel.
 */
class VersionsRepositoryImpl(
    private val client: HttpClient,
) : VersionsRepository {

    override suspend fun getVersions(projectId: String): Result<List<ProjectVersion>> = apiResult {
        apiCall { client.get(versionsPath(projectId)).body<VersionsResponseDto>() }
            .versions
            .orEmpty()
            .map { it.toDomain() }
    }

    override suspend fun getVersionUrl(projectId: String, versionId: String): Result<String> =
        apiResult {
            apiCall { client.get(versionPath(projectId, versionId)).body<VersionUrlDto>() }.toUrl()
        }

    override suspend fun restoreVersion(
        projectId: String,
        versionId: String,
    ): Result<VersionRestore> = apiResult {
        apiCall {
            client.post("${versionPath(projectId, versionId)}/$RestoreSegment") {
                setBody(EmptyJsonBody)
            }.body<VersionUrlDto>()
        }.toRestore()
    }

    private fun versionsPath(projectId: String): String =
        "$ProjectsPath/${projectId.encodeURLPathPart()}/$VersionsSegment"

    private fun versionPath(projectId: String, versionId: String): String =
        "${versionsPath(projectId)}/${versionId.encodeURLPathPart()}"
}
