package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.GenerationOutcome
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Мок диалога с ИИ — только для `@Preview` и ручной проверки вёрстки: в графе
 * Koin его больше нет, боевой источник — [ChatRepositoryImpl].
 *
 * @param networkDelayMillis имитация задержки загрузки — чтобы видеть Loading.
 * @param sendDelayMillis имитация задержки генерации.
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
     * Новую версию мок не создаёт: генерация — задача бэкенда, а `@Preview`
     * ленту всё равно не перезапрашивает. Отдаём выдуманный `job_id`.
     */
    override suspend fun startGeneration(projectId: String, prompt: String): Result<String> {
        delay(sendDelayMillis)
        return Result.success(MOCK_JOB_ID)
    }

    override suspend fun awaitGeneration(
        projectId: String,
        jobId: String,
    ): Result<GenerationOutcome> {
        delay(sendDelayMillis)
        return Result.success(GenerationOutcome.Done(previewUrl = null))
    }

    companion object {

        const val DEFAULT_TITLE: String = "Orion Brand AI"

        /** Задачи у мока нет — `job_id` нужен только чтобы закрыть контракт. */
        const val MOCK_JOB_ID: String = "mock-job"

        /**
         * Лента ровно по макету: все четыре вида сообщений видны сразу.
         * Порядок «ответ ИИ → разделитель версии» — тот же, что собирает
         * [buildChatTimeline] из настоящей истории.
         */
        fun defaultMessages(now: Instant = Instant.now()): List<ChatMessage> = listOf(
            ChatMessage.System.ProjectCreated(id = "system-created"),
            ChatMessage.Outgoing(
                id = "out-1",
                text = "Can you generate a modern coral branding kit for my AI startup?",
                sentAt = now.minus(12, ChronoUnit.MINUTES),
            ),
            ChatMessage.Incoming(
                id = "in-1",
                versionId = "version-1",
                sentAt = now.minus(11, ChronoUnit.MINUTES),
                previewUrl = "https://placehold.co/248x100/F8604A/FFFFFF/png",
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
                previewUrl = "https://placehold.co/248x100/FE9693/FFFFFF/png",
            ),
            ChatMessage.System.VersionSaved(
                id = "system-version-2",
                version = "2",
                versionId = "version-2",
                createdAt = now.minus(3, ChronoUnit.MINUTES),
            ),
        )

        /** Лента с идущей генерацией: под последним промптом — скелетон ответа. */
        fun generatingMessages(now: Instant = Instant.now()): List<ChatMessage> =
            defaultMessages(now) + listOf(
                ChatMessage.Outgoing(
                    id = "out-3",
                    text = "Now try a darker background.",
                    sentAt = now.minus(1, ChronoUnit.MINUTES),
                ),
                ChatMessage.Pending(id = "pending-1", jobId = MOCK_JOB_ID),
            )

        /** Лента с провалившейся генерацией: пузырь ошибки с «Повторить». */
        fun failedMessages(now: Instant = Instant.now()): List<ChatMessage> =
            defaultMessages(now) + listOf(
                ChatMessage.Outgoing(
                    id = "out-3",
                    text = "Now try a darker background.",
                    sentAt = now.minus(2, ChronoUnit.MINUTES),
                ),
                ChatMessage.Failed(
                    id = "failed-1",
                    text = "Generation service is temporarily unavailable. Please try again later.",
                    sentAt = now.minus(1, ChronoUnit.MINUTES),
                ),
            )
    }
}
