package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import com.yandex.avatargenerator.feature.project.domain.model.Project

/**
 * Репозиторий-подделка для тестов ViewModel.
 *
 * @param projectResult что вернуть на очередной [getProject] — успех или ошибка;
 *  поле `var`, чтобы в тесте retry подменить ошибку успехом.
 */
internal class FakeProjectRepository(
    var projectResult: Result<Project> = Result.success(DEFAULT_PROJECT),
) : ProjectRepository {

    var getProjectCallCount: Int = 0
        private set

    override suspend fun getProject(id: String): Result<Project> {
        getProjectCallCount++
        return projectResult
    }

    override suspend fun updateProject(
        id: String,
        isPublic: Boolean?,
        data: String?,
    ): Result<Unit> = Result.success(Unit)

    override suspend fun deleteProject(id: String): Result<Unit> = Result.success(Unit)

    companion object {
        val DEFAULT_PROJECT = Project(
            id = "project-1",
            authorId = "author-1",
            name = "Project",
            description = null,
            isPublic = false,
            createdAt = null,
            url = null,
            data = null,
        )
    }
}
