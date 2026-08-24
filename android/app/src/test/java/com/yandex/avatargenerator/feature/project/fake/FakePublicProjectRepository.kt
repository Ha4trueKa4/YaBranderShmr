package com.yandex.avatargenerator.feature.project.fake

import com.yandex.avatargenerator.feature.project.data.MockPublicProjectRepository
import com.yandex.avatargenerator.feature.project.domain.PublicProjectRepository
import com.yandex.avatargenerator.feature.project.domain.model.PublicProject

/**
 * Репозиторий-подделка для тестов
 * [com.yandex.avatargenerator.feature.project.ui.publicproject.PublicProjectViewModel].
 *
 * @param result что вернуть на очередной вызов; поле `var`, чтобы в тесте retry
 *  подменить ошибку успехом.
 */
internal class FakePublicProjectRepository(
    var result: Result<PublicProject> = Result.success(MockPublicProjectRepository.sample()),
) : PublicProjectRepository {

    var callCount: Int = 0
        private set

    override suspend fun getPublicProject(projectId: String): Result<PublicProject> {
        callCount++
        return result
    }
}
