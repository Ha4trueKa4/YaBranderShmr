package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import com.yandex.avatargenerator.feature.project.domain.model.Project
import com.yandex.avatargenerator.feature.project.domain.model.ProjectCreation

/**
 * Репозиторий-подделка для тестов ViewModel — и экрана проекта, и формы
 * создания (`feature/createproject` ходит в этот же интерфейс).
 *
 * @param projectResult что вернуть на очередной [getProject] — успех или ошибка;
 *  поле `var`, чтобы в тесте retry подменить ошибку успехом.
 * @param createResult что вернуть на [createProject] — успех или ошибка.
 * @param updateResult что вернуть на [updateProject] — успех или ошибка; поле
 *  `var`, чтобы проверять оптимистичный откат видимости.
 */
internal class FakeProjectRepository(
    var projectResult: Result<Project> = Result.success(DEFAULT_PROJECT),
    var createResult: Result<ProjectCreation> = Result.success(DEFAULT_CREATION),
    var updateResult: Result<Unit> = Result.success(Unit),
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
    var lastCreatedPrompt: String? = null
        private set

    /** Модель, с которой ушло создание; `null` — форма её не прислала. */
    var lastCreatedModel: String? = null
        private set

    var updateProjectCallCount: Int = 0
        private set

    /** Аргумент видимости последнего [updateProject] — проверяем, что PATCH ушёл. */
    var lastUpdatedIsPublic: Boolean? = null
        private set

    override suspend fun createProject(
        name: String,
        description: String,
        prompt: String,
        model: String?,
    ): Result<ProjectCreation> {
        createProjectCallCount++
        lastCreatedName = name
        lastCreatedDescription = description
        lastCreatedPrompt = prompt
        lastCreatedModel = model
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
    ): Result<Unit> {
        updateProjectCallCount++
        lastUpdatedIsPublic = isPublic
        return updateResult
    }

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

        val DEFAULT_CREATION = ProjectCreation(DEFAULT_PROJECT, jobId = "job-1")
    }
}
