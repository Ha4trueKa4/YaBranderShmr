package com.yandex.avatargenerator.feature.feed.ui

import com.yandex.avatargenerator.core.mvi.BaseEffect

/**
 * Одноразовые события экрана ленты — вся навигация отсюда. Отдаются через канал,
 * а не через [FeedState], чтобы переход не повторялся на каждой рекомпозиции.
 */
sealed interface FeedEffect : BaseEffect {

    data class OpenProject(val projectId: String) : FeedEffect

    data class OpenAuthor(val authorId: String) : FeedEffect
}
