package com.yandex.avatargenerator.feature.subscription.ui

import com.yandex.avatargenerator.core.mvi.BaseIntent
import com.yandex.avatargenerator.feature.subscription.domain.model.SubscriptionUser

/** Всё, что пользователь может сделать на экране подписок. */
sealed interface SubscriptionsIntent : BaseIntent {

    /** Повтор после ошибки — со скелетоном. */
    data object OnRetryClick : SubscriptionsIntent

    /**
     * Возврат на экран. Пока пользователь ходил в чужой профиль, он мог там
     * подписаться/отписаться — список перезагружаем в фоне, без скелетона.
     */
    data object OnScreenResumed : SubscriptionsIntent

    /**
     * Приехал id текущего пользователя. Отдельным интентом, а не параметром
     * конструктора: он приходит из потока и на первом кадре всегда `null`
     * (см. `rememberCurrentUser`), а конструктор ViewModel вызывается ровно раз —
     * замороженный `null` сделал бы свою строку в списке неотличимой от чужой.
     */
    data class OnCurrentUserResolved(val userId: String?) : SubscriptionsIntent

    data object OnBackClick : SubscriptionsIntent

    data class OnUserClick(val userId: String) : SubscriptionsIntent

    data class OnFollowClick(val user: SubscriptionUser) : SubscriptionsIntent
}
