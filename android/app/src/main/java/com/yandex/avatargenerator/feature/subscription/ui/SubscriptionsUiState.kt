package com.yandex.avatargenerator.feature.subscription.ui

import com.yandex.avatargenerator.feature.subscription.domain.model.SubscriptionUser

/**
 * Состояние экрана подписок. [tab] держим отдельно от [list]: переключатель
 * Followers/Following остаётся на экране, пока список под ним грузится или упал.
 */
data class SubscriptionsUiState(
    val tab: SubscriptionsTab,
    val list: SubscriptionsListState,
)

/** Состояние списка под переключателем вкладок. */
sealed interface SubscriptionsListState {

    data object Loading : SubscriptionsListState

    data object Error : SubscriptionsListState

    data class Content(val users: List<SubscriptionUser>) : SubscriptionsListState
}
