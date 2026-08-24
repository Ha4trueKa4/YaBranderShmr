package com.yandex.avatargenerator.feature.subscription.ui

import com.yandex.avatargenerator.core.mvi.BaseState
import com.yandex.avatargenerator.feature.subscription.domain.model.SubscriptionUser

/**
 * Состояние экрана подписок: один immutable-класс со всеми флагами, как требует
 * [BaseState]. Раньше список под переключателем был отдельной sealed-иерархией
 * `SubscriptionsListState`.
 *
 * [tab] держим рядом с флагами списка намеренно: заголовок Followers/Following
 * остаётся на экране, пока список под ним грузится или упал.
 *
 * @property currentUserId id текущего пользователя: на себя не подписываются,
 *  его строка рисуется без кнопки. `null` — id недоступен. Раньше он приходил
 *  и во ViewModel, и отдельно в UI; теперь источник один.
 */
data class SubscriptionsState(
    val tab: SubscriptionsTab,
    val isLoading: Boolean = true,
    val users: List<SubscriptionUser> = emptyList(),
    val hasError: Boolean = false,
    val currentUserId: String? = null,
) : BaseState {

    /** Список пуст — не ошибка: на пользователя просто никто не подписан. */
    val isEmpty: Boolean get() = !isLoading && !hasError && users.isEmpty()
}
