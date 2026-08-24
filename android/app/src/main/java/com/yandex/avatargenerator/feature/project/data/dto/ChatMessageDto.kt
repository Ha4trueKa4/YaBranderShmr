package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Запись истории диалога (`GET /projects/{id}/chat`). Бэкенд пишет их парами:
 * промпт пользователя (`role = user`, есть [text]) и ответ ИИ
 * (`role = assistant`) — у ответа либо [versionId] (генерация удалась), либо
 * [error] с текстом (упала), либо [status] = `cancelled` (генерацию прервали).
 * Своего текста у ответа нет ни в одном из случаев.
 *
 * Промпт попадает в историю **сразу при старте задачи**, до обращения к модели.
 * Пока задача считается, бэкенд дописывает в **ответ** (в БД такой записи нет)
 * заглушку `{role: assistant, status: pending|cancelling, job_id}` — по ней
 * клиент и узнаёт, что генерация ещё идёт, даже зайдя в чат заново.
 *
 * [role] — строкой, а не enum: неизвестную роль надо уметь пропустить, а
 * `kotlinx.serialization` уронил бы разбор всего ответа ещё до маппера.
 *
 * Своего `id` у записи нет — ключ для `LazyColumn` собирает маппер из индекса.
 * Все поля nullable: `required` в контракте не проставлен.
 *
 * @property status у ответа ИИ: `cancelled` — запись о прерванной генерации,
 *  она лежит в базе. `pending` и `cancelling` — служебная заглушка идущей
 *  задачи, которую бэкенд подставляет в конец ленты на лету и в базе не
 *  хранит; из неё маппер делает скелетон ожидания.
 * @property jobId общий у промпта и ответа на него: по нему клиент находит
 *  результат своей генерации, если статус-ручка потеряла задачу (см.
 *  `ChatRepositoryImpl.awaitGeneration`), и возобновляет ожидание незакрытой
 *  задачи после перезахода в чат.
 */
@Serializable
data class ChatMessageDto(
    @SerialName("role") val role: String? = null,
    @SerialName("text") val text: String? = null,
    @SerialName("version_id") val versionId: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("job_id") val jobId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
