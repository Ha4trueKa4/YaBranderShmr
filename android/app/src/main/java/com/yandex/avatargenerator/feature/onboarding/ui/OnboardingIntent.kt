package com.yandex.avatargenerator.feature.onboarding.ui

import com.yandex.avatargenerator.core.mvi.BaseIntent

internal sealed interface OnboardingIntent : BaseIntent {

    /** Пользователь долистал пейджер свайпом. */
    data class OnPageChange(val index: Int) : OnboardingIntent

    /** Единственная кнопка внизу: «Next» на слайдах 1–3 и «Get Started» на последнем. */
    data object OnPrimaryClick : OnboardingIntent

    data object OnBackClick : OnboardingIntent
}
