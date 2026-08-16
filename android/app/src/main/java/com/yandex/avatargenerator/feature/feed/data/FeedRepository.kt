package com.yandex.avatargenerator.feature.feed.data

import com.yandex.avatargenerator.feature.feed.domain.model.FeedPage
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab

/**
 * Источник данных ленты. Боевая реализация — [NetworkFeedRepository]
 * (`GET /feed`), для разработки UI без бэкенда — [MockFeedRepository].
 *
 * Пагинация курсорная (контракт бэкенда): следующую страницу запрашиваем по
 * [FeedPage.nextCursor] предыдущей, а не по номеру.
 */
interface FeedRepository {

    /**
     * Возвращает одну страницу ленты.
     *
     * @param tab режим ленты (общая / подписки).
     * @param cursor курсор предыдущей страницы; `null` — первая страница.
     * @param limit размер страницы.
     */
    suspend fun getFeed(tab: FeedTab, cursor: String?, limit: Int): FeedPage
}
