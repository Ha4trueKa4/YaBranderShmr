package com.yandex.avatargenerator.feature.feed.fake

import com.yandex.avatargenerator.feature.feed.data.FeedRepository
import com.yandex.avatargenerator.feature.feed.domain.model.FeedPage
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab

/**
 * Репозиторий-подделка для тестов ViewModel. Пейджер здесь не крутится — тесты
 * проверяют состояние вкладки и эффекты, — поэтому отдаём пустую страницу и
 * лишь запоминаем, за какими вкладками к нам приходили.
 */
internal class FakeFeedRepository : FeedRepository {

    val requestedTabs = mutableListOf<FeedTab>()

    override suspend fun getFeed(tab: FeedTab, cursor: String?, limit: Int): FeedPage {
        requestedTabs += tab
        return FeedPage(projects = emptyList(), nextCursor = null)
    }
}
