package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.core.network.apiResult
import com.yandex.avatargenerator.feature.project.data.dto.CreateProjectRequestDto
import com.yandex.avatargenerator.feature.project.data.dto.ProjectDto
import com.yandex.avatargenerator.feature.project.data.dto.UpdateProjectRequestDto
import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import com.yandex.avatargenerator.feature.project.domain.model.Project
import com.yandex.avatargenerator.feature.project.domain.model.ProjectCreation
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.encodeURLPathPart

private const val ProjectsPath = "projects"

/**
 * Создание, чтение и изменение проекта через API. Инъектит готовый [HttpClient]
 * (свой не собирает), каждый вызов заворачивает в [apiCall], `Bearer`
 * подставляет клиент сам. Наружу отдаёт [Result]: ошибку разбирает ViewModel.
 */
class ProjectRepositoryImpl(
    private val client: HttpClient,
) : ProjectRepository {

    override suspend fun createProject(
        name: String,
        description: String,
        prompt: String,
    ): Result<ProjectCreation> = apiResult {
        val dto = apiCall {
            client.post(ProjectsPath) {
                setBody(
                    CreateProjectRequestDto(
                        name = name,
                        description = description,
                        prompt = prompt,
                    ),
                )
            }.body<ProjectDto>()
        }
        // Пустую строку в job_id считаем отсутствием задачи: ждать нечего.
        ProjectCreation(dto.toDomain(), dto.jobId?.takeIf { it.isNotBlank() })
    }

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

    private fun projectPath(id: String): String = "$ProjectsPath/${id.encodeURLPathPart()}"
}
