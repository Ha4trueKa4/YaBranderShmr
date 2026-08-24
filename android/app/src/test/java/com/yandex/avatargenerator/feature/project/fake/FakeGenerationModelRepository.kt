package com.yandex.avatargenerator.feature.project.fake

import com.yandex.avatargenerator.feature.project.domain.GenerationModelRepository
import com.yandex.avatargenerator.feature.project.domain.model.GenerationModel

/**
 * Справочник моделей и выбор пользователя — подделка для тестов
 * [com.yandex.avatargenerator.feature.project.ui.ProjectViewModel].
 *
 * @param modelsResult что вернуть на очередной [getAvailableModels]; `var`,
 *  чтобы в тесте ретрая подменить ошибку успехом.
 * @param selectedModelId «сохранённая» модель. По умолчанию — дефолт бэкенда,
 *  как у пустого хранилища.
 */
internal class FakeGenerationModelRepository(
    var modelsResult: Result<List<GenerationModel>> = Result.success(MODELS),
    var selectedModelId: String = GenerationModel.DefaultId,
) : GenerationModelRepository {

    /** Сколько раз ходили за справочником: по нему видно ленивость загрузки. */
    var modelsCallCount: Int = 0
        private set

    /** Что и в каком порядке записали в настройки. */
    val savedModelIds: MutableList<String> = mutableListOf()

    override suspend fun getAvailableModels(): Result<List<GenerationModel>> {
        modelsCallCount++
        return modelsResult
    }

    override suspend fun getSelectedModelId(): String = selectedModelId

    override suspend fun selectModel(id: String) {
        selectedModelId = id
        savedModelIds += id
    }

    companion object {
        const val LUNA_ID: String = "gpt-5.6-luna"

        /** Справочник в том же составе, что отдаёт стенд с ключом OpenAI. */
        val MODELS: List<GenerationModel> = listOf(
            GenerationModel(id = GenerationModel.DefaultId, owner = "DeepSeek"),
            GenerationModel(id = "gpt-oss-120b/latest", owner = "OpenAI"),
            GenerationModel(id = LUNA_ID, owner = "OpenAI"),
        )
    }
}
