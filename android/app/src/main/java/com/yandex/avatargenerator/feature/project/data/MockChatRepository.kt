package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Временный мок диалога с ИИ для разработки UI до готовности бэкенда:
 * отдаёт ленту из макета и имитирует сетевую задержку.
 *
 * @param networkDelayMillis имитация задержки загрузки — чтобы видеть Loading.
 * @param sendDelayMillis имитация задержки отправки.
 * @param title заголовок проекта в шапке.
 * @param messages стартовая лента; параметр вынесен наружу, чтобы в `@Preview` и
 *  при ручной проверке можно было подсунуть `emptyList()` (новый проект).
 */
class MockChatRepository(
    private val networkDelayMillis: Long = 600L,
    private val sendDelayMillis: Long = 200L,
    private val title: String = DEFAULT_TITLE,
    private val messages: List<ChatMessage> = defaultMessages(),
) : ChatRepository {

    override suspend fun getChat(projectId: String): Result<ProjectChat> {
        delay(networkDelayMillis)
        return Result.success(ProjectChat(id = projectId, title = title, messages = messages))
    }

    /**
     * Ответа ИИ мок не имитирует осознанно: генерация — задача бэкенда,
     * а экрану достаточно увидеть, что исходящее сообщение попало в ленту.
     */
    override suspend fun sendMessage(projectId: String, text: String): Result<ChatMessage> {
        delay(sendDelayMillis)
        return Result.success(
            ChatMessage.Outgoing(
                id = UUID.randomUUID().toString(),
                text = text,
                sentAt = Instant.now(),
            ),
        )
    }

    companion object {

        const val DEFAULT_TITLE: String = "Orion Brand AI"

        /** Лента ровно по макету: все четыре вида сообщений видны сразу. */
        fun defaultMessages(now: Instant = Instant.now()): List<ChatMessage> = listOf(
            ChatMessage.System.ProjectCreated(id = "system-created"),
            ChatMessage.Outgoing(
                id = "out-1",
                text = "Can you generate a modern violet branding kit for my AI startup?",
                sentAt = now.minus(12, ChronoUnit.MINUTES),
            ),
            ChatMessage.Incoming(
                id = "in-1",
                text = "I've initialized the generation! Here is a sneak peek of your Orion branding asset:",
                sentAt = now.minus(11, ChronoUnit.MINUTES),
                imageUrl = "https://placehold.co/248x100/7C3AED/FFFFFF/png",
            ),
            ChatMessage.System.VersionSaved(id = "system-version-1", version = "1.2"),
            ChatMessage.Outgoing(
                id = "out-2",
                text = "Make the mark rounder and try a cyan accent.",
                sentAt = now.minus(4, ChronoUnit.MINUTES),
            ),
            ChatMessage.Incoming(
                id = "in-2",
                text = "Done — softer geometry with a cyan highlight:",
                sentAt = now.minus(3, ChronoUnit.MINUTES),
                imageUrl = "https://placehold.co/248x100/06B6D4/FFFFFF/png",
            ),
        )
    }
}
