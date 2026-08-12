package com.yandex.avatargenerator.feature.feed.data

import com.yandex.avatargenerator.feature.feed.domain.model.FeedAuthor
import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * Временный мок ленты для разработки UI до готовности бэкенда.
 * Генерирует детерминированный набор проектов, имитирует сетевую задержку
 * и конечность ленты (см. [totalItemsFor]).
 */
class MockFeedRepository(
    private val networkDelayMillis: Long = 700L,
) : FeedRepository {

    override suspend fun getFeed(tab: FeedTab, page: Int, pageSize: Int): List<FeedProject> {
        delay(networkDelayMillis)

        val total = totalItemsFor(tab)
        val from = page * pageSize
        if (from >= total) return emptyList()
        val to = minOf(from + pageSize, total)

        return (from until to).map { index -> buildProject(tab, index) }
    }

    private fun buildProject(tab: FeedTab, index: Int): FeedProject {
        val seed = "${tab.name}-$index"
        val rnd = Random(seed.hashCode())
        val authorIndex = rnd.nextInt(AUTHOR_NAMES.size)
        val projectName = PROJECT_NAMES[rnd.nextInt(PROJECT_NAMES.size)]

        return FeedProject(
            id = seed,
            title = "$projectName #${index + 1}",
            previewUrl = "https://picsum.photos/seed/$seed/600/600",
            author = FeedAuthor(
                id = "author-$authorIndex",
                displayName = AUTHOR_NAMES[authorIndex],
                avatarUrl = "https://i.pravatar.cc/150?u=author-$authorIndex",
            ),
            publishedAt = Instant.now().minus(index.toLong(), ChronoUnit.HOURS),
        )
    }

    /**
     * «All» — наполненная лента, «Subscriptions» — пусто (демонстрирует empty-state
     * по макету: новый пользователь ещё ни на кого не подписан).
     */
    private fun totalItemsFor(tab: FeedTab): Int = when (tab) {
        FeedTab.ALL -> 57
        FeedTab.SUBSCRIPTIONS -> 0
    }

    private companion object {
        val AUTHOR_NAMES = listOf(
            "Studio Nord", "Алина Б.", "PixelForge", "Дэян К.", "Мария О.", "brand.lab",
        )
        val PROJECT_NAMES = listOf(
            "Логотип кофейни", "Айдентика стартапа", "Аватарка для чата",
            "Обложка сообщества", "Визитка", "Фирменный стиль",
        )
    }
}
