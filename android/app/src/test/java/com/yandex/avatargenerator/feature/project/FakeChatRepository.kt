package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat
import java.time.Instant

/**
 * Репозиторий-подделка для тестов ViewModel.
 *
 * @param chatResult что вернуть на очередной [getChat] — успех или ошибка;
 *  поле `var`, чтобы в тесте retry подменить ошибку успехом.
 * @param sentAt время отправленного сообщения — фиксированное, чтобы результат
 *  [sendMessage] можно было сравнить с ожидаемым.
 */
internal class FakeChatRepository(
    var chatResult: Result<ProjectChat> = Result.success(EMPTY_CHAT),
    private val sentAt: Instant = SENT_AT,
) : ChatRepository {

    var getChatCallCount: Int = 0
        private set

    /**
     * Тексты, дошедшие до репозитория: по ним видно, что отправка не сработала.
     * При заданном [sendError] текст всё равно попадает сюда — попытка отправки
     * была, упала она уже после.
     */
    val sentTexts: MutableList<String> = mutableListOf()

    /** Ошибка, которую должен бросить [sendMessage]; null — отправка успешна. */
    var sendError: Throwable? = null

    override suspend fun getChat(projectId: String): Result<ProjectChat> {
        getChatCallCount++
        return chatResult
    }

    override suspend fun sendMessage(projectId: String, text: String): Result<ChatMessage> {
        sentTexts += text
        sendError?.let { return Result.failure(it) }
        return Result.success(
            ChatMessage.Outgoing(id = "sent-${sentTexts.size}", text = text, sentAt = sentAt),
        )
    }

    companion object {
        val SENT_AT: Instant = Instant.parse("2026-08-12T11:32:00Z")
        val EMPTY_CHAT = ProjectChat(id = "project-1", title = "Project", messages = emptyList())
    }
}
