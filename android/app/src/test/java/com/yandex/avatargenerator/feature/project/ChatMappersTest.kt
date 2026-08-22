package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.feature.project.data.buildChatTimeline
import com.yandex.avatargenerator.feature.project.data.dto.ChatMessageDto
import com.yandex.avatargenerator.feature.project.data.findGenerationOutcome
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.GenerationOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Маппер истории чата. Отдельно от [ChatRepositoryImplTest]: там проверяется
 * сборка экрана из трёх ручек, здесь — разбор самих записей.
 */
class ChatMappersTest {

    private fun timeline(vararg history: ChatMessageDto): List<ChatMessage> =
        buildChatTimeline(history.toList(), latestVersionId = null, latestVersionUrl = null)

    @Test
    fun `ответ с error становится пузырём ошибки`() {
        val messages = timeline(
            ChatMessageDto(role = "user", text = "Violet logo", jobId = "job-1"),
            ChatMessageDto(role = "assistant", error = "Invalid SVG", jobId = "job-1"),
        )

        assertEquals(
            ChatMessage.Failed(id = "chat-1-assistant", text = "Invalid SVG", sentAt = null),
            messages.last(),
        )
    }

    @Test
    fun `провал не создаёт разделителя версии`() {
        // Версии у провала нет — рисовать под ним «VERSION N SAVED» нечем.
        val messages = timeline(ChatMessageDto(role = "assistant", error = "Invalid SVG"))

        assertTrue(
            "лента: $messages",
            messages.none { it is ChatMessage.System.VersionSaved },
        )
    }

    @Test
    fun `нумерация версий не сдвигается на провале`() {
        // Иначе после одной неудачи все следующие разделители врали бы на единицу.
        val messages = timeline(
            ChatMessageDto(role = "assistant", versionId = "v-2"),
            ChatMessageDto(role = "assistant", error = "Invalid SVG"),
            ChatMessageDto(role = "assistant", versionId = "v-3"),
        )

        assertEquals(
            listOf("1", "2"),
            messages.filterIsInstance<ChatMessage.System.VersionSaved>().map { it.version },
        )
    }

    @Test
    fun `ответ без version_id и без error пропускается`() {
        // Порченая строка не должна стоить пользователю всего экрана.
        val messages = timeline(ChatMessageDto(role = "assistant"))

        assertEquals(listOf<ChatMessage>(ChatMessage.System.ProjectCreated("system-created")), messages)
    }

    @Test
    fun `промпт без ответа — это идущая генерация, а не мусор`() {
        // Бэкенд пишет промпт в историю сразу при старте задачи.
        val messages = timeline(ChatMessageDto(role = "user", text = "Violet logo", jobId = "job-1"))

        assertEquals("Violet logo", (messages.last() as ChatMessage.Outgoing).text)
    }

    @Test
    fun `поиск по job_id находит удачный ответ`() {
        val history = listOf(
            ChatMessageDto(role = "user", text = "Violet logo", jobId = "job-1"),
            ChatMessageDto(role = "assistant", versionId = "v-9", jobId = "job-1"),
        )

        // Ссылки в истории нет — экран возьмёт её из перечитанной ленты.
        assertEquals(
            GenerationOutcome.Done(previewUrl = null),
            history.findGenerationOutcome("job-1"),
        )
    }

    @Test
    fun `поиск по job_id находит провал`() {
        val history = listOf(
            ChatMessageDto(role = "assistant", error = "Invalid SVG", jobId = "job-1"),
        )

        assertEquals(
            GenerationOutcome.Failed(message = "Invalid SVG"),
            history.findGenerationOutcome("job-1"),
        )
    }

    @Test
    fun `чужой job_id и промпт с нашим job_id за ответ не считаются`() {
        val history = listOf(
            // Промпт нашей задачи — ответом он не является.
            ChatMessageDto(role = "user", text = "Violet logo", jobId = "job-1"),
            ChatMessageDto(role = "assistant", versionId = "v-9", jobId = "job-2"),
        )

        assertNull(history.findGenerationOutcome("job-1"))
    }
}
