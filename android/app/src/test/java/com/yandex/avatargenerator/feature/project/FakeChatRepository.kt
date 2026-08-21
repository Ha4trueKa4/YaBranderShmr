package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.model.GeneratedVersion
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
     * Очередь ответов [getChat] — нужна тестам ожидания после таймаута: там
     * лента меняется по ходу опроса. Пока очередь не пуста, ответ берётся из
     * неё, дальше — из [chatResult].
     */
    val chatResults: MutableList<Result<ProjectChat>> = mutableListOf()

    /**
     * Промпты, дошедшие до репозитория: по ним видно, что отправка состоялась.
     * При заданном [sendError] промпт всё равно попадает сюда — попытка была,
     * упала она уже после.
     */
    val sentPrompts: MutableList<String> = mutableListOf()

    /** Ошибка, которую должен вернуть [sendPrompt]; null — генерация удалась. */
    var sendError: Throwable? = null

    override suspend fun getChat(projectId: String): Result<ProjectChat> {
        getChatCallCount++
        return if (chatResults.isEmpty()) chatResult else chatResults.removeAt(0)
    }

    override suspend fun sendPrompt(projectId: String, prompt: String): Result<GeneratedVersion> {
        sentPrompts += prompt
        sendError?.let { return Result.failure(it) }
        return Result.success(GeneratedVersion(previewUrl = GENERATED_URL))
    }

    companion object {
        const val GENERATED_URL: String = "https://s3.example/generated.svg"
        val EMPTY_CHAT = ProjectChat(
            id = "project-1",
            title = "Project",
            isPublic = false,
            messages = emptyList(),
        )
    }
}
