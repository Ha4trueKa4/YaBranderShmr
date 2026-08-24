package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.model.GenerationOutcome
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat

/**
 * Репозиторий-подделка для тестов ViewModel.
 *
 * @param chatResult что вернуть на очередной [getChat] — успех или ошибка;
 *  поле `var`, чтобы в тесте retry подменить ошибку успехом, а в тесте отправки —
 *  подсунуть ленту, какой она станет после генерации.
 */
internal class FakeChatRepository(
    var chatResult: Result<ProjectChat> = Result.success(EMPTY_CHAT),
) : ChatRepository {

    var getChatCallCount: Int = 0
        private set

    /**
     * Очередь ответов [getChat] — нужна тестам, где лента меняется по ходу
     * сценария. Пока очередь не пуста, ответ берётся из неё, дальше — из
     * [chatResult].
     */
    val chatResults: MutableList<Result<ProjectChat>> = mutableListOf()

    /**
     * Промпты, дошедшие до репозитория: по ним видно, что отправка состоялась.
     * При заданном [startError] промпт всё равно попадает сюда — попытка была,
     * упала она уже после.
     */
    val sentPrompts: MutableList<String> = mutableListOf()

    /** Модели, с которыми ушли промпты, — по одной на каждый [sentPrompts]. */
    val sentModels: MutableList<String?> = mutableListOf()

    /** Задачи, результат которых ждали: по ним видно, что опрос запускался. */
    val awaitedJobIds: MutableList<String> = mutableListOf()

    /** Ошибка [startGeneration]; `null` — задача поставлена. */
    var startError: Throwable? = null

    /** Чем закончится ожидание. По умолчанию — удачная генерация. */
    var awaitResult: Result<GenerationOutcome> =
        Result.success(GenerationOutcome.Done(previewUrl = GENERATED_URL))

    override suspend fun getChat(projectId: String): Result<ProjectChat> {
        getChatCallCount++
        return if (chatResults.isEmpty()) chatResult else chatResults.removeAt(0)
    }

    override suspend fun startGeneration(
        projectId: String,
        prompt: String,
        model: String?,
    ): Result<String> {
        sentPrompts += prompt
        sentModels += model
        startError?.let { return Result.failure(it) }
        return Result.success(JOB_ID)
    }

    override suspend fun awaitGeneration(
        projectId: String,
        jobId: String,
    ): Result<GenerationOutcome> {
        awaitedJobIds += jobId
        return awaitResult
    }

    companion object {
        const val GENERATED_URL: String = "https://s3.example/generated.svg"
        const val JOB_ID: String = "job-1"
        val EMPTY_CHAT = ProjectChat(
            id = "project-1",
            title = "Project",
            isPublic = false,
            messages = emptyList(),
        )
    }
}
