package com.yandex.avatargenerator.feature.feed.domain.model

/**
 * Одна страница ленты при курсорной пагинации.
 *
 * @property nextCursor курсор для следующей страницы; `null` — это последняя.
 */
data class FeedPage(
    val projects: List<FeedProject>,
    val nextCursor: String?,
)
