package com.yandex.avatargenerator.feature.feed.data

import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab

/**
 * Источник данных ленты. Пока реализован моком [MockFeedRepository];
 * при готовности бэкенда добавится сетевая реализация (Retrofit) без изменения UI.
 */
interface FeedRepository {

    /**
     * Возвращает одну страницу ленты.
     *
     * @param tab режим ленты (общая / подписки).
     * @param page номер страницы, начиная с 0.
     * @param pageSize размер страницы.
     * @return проекты страницы; пустой список означает конец ленты.
     */
    suspend fun getFeed(tab: FeedTab, page: Int, pageSize: Int): List<FeedProject>
}
