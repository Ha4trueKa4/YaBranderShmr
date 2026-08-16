package com.yandex.avatargenerator.feature.feed.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Ответ `GET /feed` — страница ленты с курсором на следующую. */
@Serializable
data class FeedResponseDto(
    @SerialName("items") val items: List<FeedItemDto>? = null,
    /** Отсутствует, если страница последняя. */
    @SerialName("next_cursor") val nextCursor: String? = null,
)

/** Элемент ленты (`FeedItem`): публичный проект без холста. */
@Serializable
data class FeedItemDto(
    @SerialName("id") val id: String? = null,
    @SerialName("author_id") val authorId: String? = null,
    @SerialName("author_login") val authorLogin: String? = null,
    @SerialName("name") val name: String? = null,
    /** ISO 8601 date-time строкой; парсинг в дату — на стороне маппера. */
    @SerialName("created_at") val createdAt: String? = null,
    /** Ссылка на превью; пустая строка, если превью ещё не сгенерировано. */
    @SerialName("preview_url") val previewUrl: String? = null,
)
