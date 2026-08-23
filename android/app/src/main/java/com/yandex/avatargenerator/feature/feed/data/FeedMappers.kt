package com.yandex.avatargenerator.feature.feed.data

import com.yandex.avatargenerator.core.data.toInstantOrNull
import com.yandex.avatargenerator.feature.feed.data.dto.FeedItemDto
import com.yandex.avatargenerator.feature.feed.data.dto.FeedResponseDto
import com.yandex.avatargenerator.feature.feed.domain.model.FeedAuthor
import com.yandex.avatargenerator.feature.feed.domain.model.FeedPage
import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab

/** Значение параметра `scope` для бэкенда. */
internal val FeedTab.scope: String
    get() = when (this) {
        FeedTab.ALL -> "discover"
        FeedTab.SUBSCRIPTIONS -> "following"
    }

/** DTO выше `data/` не поднимаются: маппинг живёт здесь. */
internal fun FeedResponseDto.toDomain(): FeedPage = FeedPage(
    // Битый элемент пропускаем, а не роняем всю страницу: одна кривая запись не
    // должна прятать остальную ленту.
    projects = items.orEmpty().mapNotNull { it.toDomainOrNull() },
    // Пустой курсор трактуем как его отсутствие: следующей страницы нет.
    nextCursor = nextCursor?.takeIf { it.isNotBlank() },
)

private fun FeedItemDto.toDomainOrNull(): FeedProject? {
    val id = id ?: return null
    val name = name ?: return null
    val authorId = authorId ?: return null
    val authorLogin = authorLogin ?: return null
    return FeedProject(
        id = id,
        title = name,
        // Пустая строка допустима по контракту — превью ещё не сгенерировано.
        previewUrl = previewUrl.orEmpty(),
        author = FeedAuthor(id = authorId, displayName = authorLogin, avatarUrl = null),
        publishedAt = createdAt?.toInstantOrNull(),
    )
}
