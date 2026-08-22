package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Запись истории диалога (`GET /projects/{id}/chat`). Бэкенд пишет их парами:
 * промпт пользователя (`role = user`, есть `text`) и ответ ИИ
 * (`role = assistant`, есть только `version_id` — ни текста, ни ссылки).
 *
 * [role] — строкой, а не enum: неизвестную роль надо уметь пропустить, а
 * `kotlinx.serialization` уронил бы разбор всего ответа ещё до маппера.
 *
 * Своего `id` у записи нет — ключ для `LazyColumn` собирает маппер из индекса.
 * Все поля nullable: `required` в контракте не проставлен.
 */
@Serializable
data class ChatMessageDto(
    @SerialName("role") val role: String? = null,
    @SerialName("text") val text: String? = null,
    @SerialName("version_id") val versionId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
