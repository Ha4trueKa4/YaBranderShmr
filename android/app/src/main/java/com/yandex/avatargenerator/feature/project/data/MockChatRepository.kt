package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Мок диалога с ИИ — только для `@Preview` и ручной проверки вёрстки: в графе
 * Koin его больше нет, боевой источник — [ChatRepositoryImpl].
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
        return Result.success(
            ProjectChat(id = projectId, title = title, isPublic = false, messages = messages),
        )
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

        /**
         * Лента ровно по макету: все четыре вида сообщений видны сразу.
         * Порядок «ответ ИИ → разделитель версии» — тот же, что собирает
         * [buildChatTimeline] из настоящей истории.
         */
        fun defaultMessages(now: Instant = Instant.now()): List<ChatMessage> = listOf(
            ChatMessage.System.ProjectCreated(id = "system-created"),
            ChatMessage.Outgoing(
                id = "out-1",
                text = "Can you generate a modern violet branding kit for my AI startup?",
                sentAt = now.minus(12, ChronoUnit.MINUTES),
            ),
            ChatMessage.Incoming(
                id = "in-1",
                versionId = "version-1",
                sentAt = now.minus(11, ChronoUnit.MINUTES),
                previewUrl = "https://placehold.co/248x100/7C3AED/FFFFFF/png",
            ),
            ChatMessage.System.VersionSaved(
                id = "system-version-1",
                version = "1",
                versionId = "version-1",
                createdAt = now.minus(11, ChronoUnit.MINUTES),
            ),
            ChatMessage.Outgoing(
                id = "out-2",
                text = "Make the mark rounder and try a cyan accent.",
                sentAt = now.minus(4, ChronoUnit.MINUTES),
            ),
            ChatMessage.Incoming(
                id = "in-2",
                versionId = "version-2",
                sentAt = now.minus(3, ChronoUnit.MINUTES),
                previewUrl = "https://placehold.co/248x100/06B6D4/FFFFFF/png",
            ),
            ChatMessage.System.VersionSaved(
                id = "system-version-2",
                version = "2",
                versionId = "version-2",
                createdAt = now.minus(3, ChronoUnit.MINUTES),
            ),
        )
    }
}
