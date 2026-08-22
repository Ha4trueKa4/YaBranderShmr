package com.yandex.avatargenerator.feature.project.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ `GET /projects/{id}/chat` — история завёрнута в объект, порядок
 * добавления (старые сверху). Пагинации нет: массив приходит целиком.
 *
 * Поле nullable: отсутствие `chat_history` маппер трактует как пустую историю —
 * пользователю «истории нет» и «поля нет» выглядят одинаково.
 */
@Serializable
data class ChatHistoryResponseDto(
    @SerialName("chat_history") val chatHistory: List<ChatMessageDto>? = null,
)
