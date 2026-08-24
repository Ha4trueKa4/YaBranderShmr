package com.yandex.avatargenerator.feature.project.data

import com.yandex.avatargenerator.core.data.toInstantOrNull
import com.yandex.avatargenerator.feature.project.data.dto.ChatMessageDto
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.GenerationOutcome
import java.time.Instant

private const val UserRole = "user"
private const val AssistantRole = "assistant"

/** Единственный статус ответа ИИ, который лежит в базе: генерацию прервали. */
private const val CancelledStatus = "cancelled"

/**
 * Статусы **идущей** задачи. Такую запись бэкенд подставляет в конец ленты на
 * лету, в базе её нет. `cancelling` — отмену запросили, но фоновая корутина её
 * ещё не заметила; для ленты это та же недосчитанная генерация.
 */
private val RunningStatuses = setOf("pending", "cancelling")

/** Ключ системной пилюли: она в ленте одна, индекс ей не нужен. */
private const val ProjectCreatedId = "system-created"

/**
 * Собирает ленту экрана из истории диалога.
 *
 * Кривую запись — неизвестная роль, промпт без текста, ответ без `version_id`,
 * `error` и известного `status` — пропускаем, а не роняем разбор: одна плохая
 * строка не должна стоить пользователю всего экрана.
 *
 * Промпт без ответа под ним ошибкой не считается: бэкенд пишет его в историю
 * сразу при старте задачи. Пока задача считается, следом идёт заглушка
 * `status = pending` — она и становится скелетоном ответа.
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

    // Номер разделителя — по порядку удачных ответов ИИ, а не version_num из
    // базы: версия 1 создаётся вместе с проектом и в историю чата не попадает.
    // Провал версии не создаёт и номер не двигает — разделитель под ним не рисуем.
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
                val versionId = record.versionId
                if (versionId == null) {
                    add(
                        record.assistantWithoutVersion(
                            id = "chat-$index-$AssistantRole",
                            createdAt = createdAt,
                        ) ?: return@forEachIndexed,
                    )
                    return@forEachIndexed
                }
                answerNumber++
                add(
                    ChatMessage.Incoming(
                        id = "chat-$index-$AssistantRole",
                        versionId = versionId,
                        sentAt = createdAt,
                        previewUrl = latestVersionUrl.takeIf { versionId == latestVersionId },
                        jobId = record.jobId,
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

/**
 * Ответ ИИ, за которым не стоит версии: провал генерации, её отмена либо
 * скелетон ещё идущей задачи.
 *
 * Заглушку идущей задачи в ленту пускаем именно здесь: своего скелетона у
 * экрана, открытого заново во время генерации, ещё нет — опрос оборвался вместе
 * с прошлым `viewModelScope`. Выброси её, и в ленте остался бы один
 * «осиротевший» промпт, а экран не узнал бы, какую задачу ждать и какую можно
 * остановить.
 *
 * `null` — показывать нечего: запись кривая (нет ни `error`, ни известного
 * `status`) либо у заглушки нет `job_id`, а без него скелетон бесполезен.
 */
private fun ChatMessageDto.assistantWithoutVersion(
    id: String,
    createdAt: Instant?,
): ChatMessage? = when {
    error != null -> ChatMessage.Failed(id = id, text = error, sentAt = createdAt, jobId = jobId)
    status == CancelledStatus -> ChatMessage.Cancelled(id = id, sentAt = createdAt, jobId = jobId)
    status in RunningStatuses -> jobId?.let { ChatMessage.Pending(id = id, jobId = it) }
    else -> null
}

/**
 * Ищет в истории ответ ИИ на задачу [jobId].
 *
 * Нужен, когда статус-ручка отвечает 404: задача живёт в памяти бэкенда, и его
 * перезапуск теряет её — уже после того, как генерация могла успешно
 * завершиться и попасть в историю. `job_id` общий у промпта и ответа, так что
 * результат своей генерации клиент узнаёт даже без задачи на сервере.
 *
 * @return исход генерации; `null` — ответа с таким `job_id` в истории ещё нет.
 */
internal fun List<ChatMessageDto>.findGenerationOutcome(jobId: String): GenerationOutcome? =
    firstNotNullOfOrNull { record ->
        if (record.role != AssistantRole || record.jobId != jobId) return@firstNotNullOfOrNull null
        when {
            record.versionId != null -> GenerationOutcome.Done(previewUrl = null)
            record.error != null -> GenerationOutcome.Failed(message = record.error)
            record.status == CancelledStatus -> GenerationOutcome.Cancelled
            // Служебная заглушка pending/cancelling: задача ещё идёт, исхода нет.
            else -> null
        }
    }
