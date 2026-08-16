package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.feature.project.data.dto.ChatMessageDto
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage

private const val UserRole = "user"
private const val AssistantRole = "assistant"

/** Ключ системной пилюли: она в ленте одна, индекс ей не нужен. */
private const val ProjectCreatedId = "system-created"

/**
 * Собирает ленту экрана из истории диалога.
 *
 * Кривую запись — неизвестная роль, промпт без текста, ответ без `version_id` —
 * пропускаем, а не роняем разбор: одна плохая строка не должна стоить
 * пользователю всего экрана.
 *
 * @param latestVersionId версия, на которую указывает [latestVersionUrl] —
 *  первый элемент `GET /versions` (сервер отдаёт их по убыванию номера).
 * @param latestVersionUrl `url` из `GET /projects/{id}`, то есть ссылка на
 *  последнюю версию. Ссылок на остальные версии в истории нет принципиально
 *  (presigned живёт час) — их запрашивают по требованию по [ChatMessage.Incoming.versionId].
 */
internal fun buildChatTimeline(
    history: List<ChatMessageDto>,
    latestVersionId: String?,
    latestVersionUrl: String?,
): List<ChatMessage> = buildList {
    add(ChatMessage.System.ProjectCreated(id = ProjectCreatedId))

    // Номер разделителя — по порядку ответов ИИ, а не version_num из базы:
    // версия 1 создаётся вместе с проектом и в историю чата не попадает.
    var answerNumber = 0

    history.forEachIndexed { index, record ->
        val createdAt = record.createdAt?.toInstantOrNull()
        when (record.role) {
            UserRole -> {
                val text = record.text ?: return@forEachIndexed
                add(
                    ChatMessage.Outgoing(
                        id = "chat-$index-$UserRole",
                        text = text,
                        sentAt = createdAt,
                    ),
                )
            }

            AssistantRole -> {
                val versionId = record.versionId ?: return@forEachIndexed
                answerNumber++
                add(
                    ChatMessage.Incoming(
                        id = "chat-$index-$AssistantRole",
                        versionId = versionId,
                        sentAt = createdAt,
                        previewUrl = latestVersionUrl.takeIf { versionId == latestVersionId },
                    ),
                )
                add(
                    ChatMessage.System.VersionSaved(
                        id = "chat-$index-version",
                        version = answerNumber.toString(),
                        versionId = versionId,
                        createdAt = createdAt,
                    ),
                )
            }
        }
    }
}
