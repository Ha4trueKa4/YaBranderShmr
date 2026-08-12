package com.yandex.avatargenerator.feature.feed.domain.model

import java.time.Instant

/**
 * Публичный проект (брендбук) в ленте.
 *
 * @property previewUrl растеризованное превью проекта (PNG), отдаётся бэкендом.
 * @property publishedAt момент публикации, для сортировки и отображения даты.
 */
data class FeedProject(
    val id: String,
    val title: String,
    val previewUrl: String,
    val author: FeedAuthor,
    val publishedAt: Instant,
)
