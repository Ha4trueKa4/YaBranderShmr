package com.yandex.avatargenerator.feature.createproject.data

import com.yandex.avatargenerator.feature.createproject.domain.CreateProjectRepository
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Временный мок создания проекта для разработки UI до готовности бэкенда:
 * имитирует задержку генерации и возвращает свежий id.
 *
 * @param generationDelayMillis имитация работы LLM — чтобы видеть состояние
 *  «Generating…» на кнопке.
 */
class MockCreateProjectRepository(
    private val generationDelayMillis: Long = 1500L,
) : CreateProjectRepository {

    override suspend fun createProject(name: String, description: String): String {
        delay(generationDelayMillis)
        return "project-${UUID.randomUUID()}"
    }
}
