package com.yandex.avatargenerator.feature.createproject

import com.yandex.avatargenerator.feature.createproject.domain.CreateProjectRepository

/**
 * Репозиторий-подделка для тестов ViewModel создания проекта.
 *
 * @param newProjectId id, который вернуть на создание проекта.
 * @param shouldThrow имитировать сбой генерации (сеть/бэкенд) — бросить исключение.
 */
internal class FakeCreateProjectRepository(
    var newProjectId: String = "project-1",
    var shouldThrow: Boolean = false,
) : CreateProjectRepository {

    var callCount: Int = 0
        private set

    /** Аргументы последнего вызова — для проверки, что они дошли до репозитория. */
    var lastName: String? = null
        private set
    var lastDescription: String? = null
        private set

    override suspend fun createProject(name: String, description: String): String {
        callCount++
        lastName = name
        lastDescription = description
        if (shouldThrow) error("generation failed")
        return newProjectId
    }
}
