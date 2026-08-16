package com.yandex.avatargenerator.feature.feed.domain.model

import java.time.Instant

/**
 * Публичный проект (брендбук) в ленте.
 *
 * @property previewUrl превью последней версии проекта (SVG из S3); пустая
 *   строка, если превью ещё не сгенерировано.
 * @property publishedAt момент публикации, для сортировки и отображения даты.
 */
data class FeedProject(
    val id: String,
    val title: String,
    val previewUrl: String,
    val author: FeedAuthor,
    val publishedAt: Instant,
)
