package com.yandex.avatargenerator.feature.profile.ui

import com.yandex.avatargenerator.core.mvi.BaseEffect

/**
 * Одноразовые события экрана профиля — вся навигация отсюда. Отдаются через
 * канал, а не через [ProfileState], чтобы переход не повторялся на каждой
 * рекомпозиции.
 */
sealed interface ProfileEffect : BaseEffect {

    data class OpenProject(val projectId: String) : ProfileEffect

    data class OpenFollowers(val userId: String) : ProfileEffect

    data class OpenFollowing(val userId: String) : ProfileEffect

    data object OpenSettings : ProfileEffect

    /** Кнопка действия на своём профиле — редактирование профиля. */
    data object OpenEditProfile : ProfileEffect

    data object NavigateBack : ProfileEffect
}
