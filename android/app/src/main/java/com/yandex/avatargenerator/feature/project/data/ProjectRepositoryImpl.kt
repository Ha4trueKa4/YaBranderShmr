package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.core.network.apiResult
import com.yandex.avatargenerator.feature.project.data.dto.ProjectDto
import com.yandex.avatargenerator.feature.project.data.dto.UpdateProjectRequestDto
import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import com.yandex.avatargenerator.feature.project.domain.model.Project
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.encodeURLPathPart

private const val ProjectsPath = "projects"

/**
 * Чтение и изменение одного проекта через API. Инъектит готовый [HttpClient]
 * (свой не собирает), каждый вызов заворачивает в [apiCall], `Bearer`
 * подставляет клиент сам. Наружу отдаёт [Result]: ошибку разбирает ViewModel.
 */
class ProjectRepositoryImpl(
    private val client: HttpClient,
) : ProjectRepository {

    override suspend fun getProject(id: String): Result<Project> = apiResult {
        apiCall { client.get(projectPath(id)).body<ProjectDto>() }.toDomain()
    }

    override suspend fun updateProject(
        id: String,
        isPublic: Boolean?,
        data: String?,
    ): Result<Unit> = apiResult {
        apiCall {
            client.patch(projectPath(id)) {
                setBody(UpdateProjectRequestDto(isPublic = isPublic, data = data))
            }
        }
    }

    override suspend fun deleteProject(id: String): Result<Unit> = apiResult {
        apiCall { client.delete(projectPath(id)) }
    }

    // id может прийти из ссылки/навигации — кодируем сегмент, чтобы спецсимвол
    // не сломал путь запроса.
    private fun projectPath(id: String): String = "$ProjectsPath/${id.encodeURLPathPart()}"
}
