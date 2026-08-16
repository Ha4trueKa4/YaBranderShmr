package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import com.yandex.avatargenerator.feature.project.domain.model.Project

/**
 * Репозиторий-подделка для тестов ViewModel — и экрана проекта, и формы
 * создания (`feature/createproject` ходит в этот же интерфейс).
 *
 * @param projectResult что вернуть на очередной [getProject] — успех или ошибка;
 *  поле `var`, чтобы в тесте retry подменить ошибку успехом.
 * @param createResult что вернуть на [createProject] — успех или ошибка.
 */
internal class FakeProjectRepository(
    var projectResult: Result<Project> = Result.success(DEFAULT_PROJECT),
    var createResult: Result<Project> = Result.success(DEFAULT_PROJECT),
) : ProjectRepository {

    var getProjectCallCount: Int = 0
        private set

    var createProjectCallCount: Int = 0
        private set

    /** Аргументы последнего [createProject] — проверяем, что дошли до репозитория. */
    var lastCreatedName: String? = null
        private set
    var lastCreatedDescription: String? = null
        private set

    override suspend fun createProject(name: String, description: String): Result<Project> {
        createProjectCallCount++
        lastCreatedName = name
        lastCreatedDescription = description
        return createResult
    }

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
