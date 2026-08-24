package com.yandex.avatargenerator.feature.subscription.ui

import com.yandex.avatargenerator.core.mvi.BaseEffect

/**
 * Одноразовые события экрана подписок — вся навигация отсюда. Отдаются через
 * канал, а не через [SubscriptionsState], чтобы переход не повторялся на каждой
 * рекомпозиции.
 */
sealed interface SubscriptionsEffect : BaseEffect {

    data object NavigateBack : SubscriptionsEffect

    data class OpenUserProfile(val userId: String) : SubscriptionsEffect

    /**
     * Тап по своей строке ведёт на вкладку своего профиля (с кнопкой
     * «Редактировать»), а не на чужой профиль с «Подписаться».
     */
    data object OpenOwnProfile : SubscriptionsEffect
}
