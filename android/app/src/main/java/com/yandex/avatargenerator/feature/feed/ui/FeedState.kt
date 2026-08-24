package com.yandex.avatargenerator.feature.feed.ui

import com.yandex.avatargenerator.core.mvi.BaseState
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab

/**
 * Состояние экрана ленты — только выбранная вкладка.
 *
 * Сам список сюда не кладётся: он приходит из Paging 3 отдельным потоком
 * (`FeedViewModel.pagingData`), а `PagingData` мутабелен и не сравним, то есть
 * в immutable-состояние не укладывается по определению. Флаги загрузки и ошибки
 * списка тоже живут в Paging (`LazyPagingItems.loadState`) — дублировать их
 * здесь значило бы держать две версии правды. Это единственное место в
 * кодовой базе, где состояние экрана шире одного [BaseState].
 */
data class FeedState(
    val selectedTab: FeedTab = FeedTab.ALL,
) : BaseState
