package com.yandex.avatargenerator.feature.profile.ui

import com.yandex.avatargenerator.core.mvi.BaseIntent

/** Всё, что пользователь может сделать на экране профиля. */
sealed interface ProfileIntent : BaseIntent {

    /** Повтор после ошибки. */
    data object OnRetryClick : ProfileIntent

    /**
     * Кнопка в шапке. Что она делает, решает ViewModel: на своём профиле это
     * «Edit profile», на чужом — подписаться/отписаться.
     */
    data object OnActionClick : ProfileIntent

    data class OnProjectClick(val projectId: String) : ProfileIntent

    data object OnFollowersClick : ProfileIntent

    data object OnFollowingClick : ProfileIntent

    data object OnSettingsClick : ProfileIntent

    data object OnBackClick : ProfileIntent
}
