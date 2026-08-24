package com.yandex.avatargenerator.feature.feed.ui

import com.yandex.avatargenerator.core.mvi.BaseIntent
import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab

/** Всё, что пользователь может сделать на экране ленты. */
sealed interface FeedIntent : BaseIntent {

    data class OnTabSelected(val tab: FeedTab) : FeedIntent

    data class OnProjectClick(val projectId: String) : FeedIntent

    data class OnAuthorClick(val authorId: String) : FeedIntent

    /** «Смотреть проекты» из пустой ленты подписок — уводит на общую вкладку. */
    data object OnExploreClick : FeedIntent
}
