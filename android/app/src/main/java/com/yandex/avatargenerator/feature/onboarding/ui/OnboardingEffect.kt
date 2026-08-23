package com.yandex.avatargenerator.feature.onboarding.ui

import com.yandex.avatargenerator.core.mvi.BaseEffect

/** Одноразовые события для навигации наружу фичи. */
internal sealed interface OnboardingEffect : BaseEffect {
    data object NavigateToHome : OnboardingEffect
}
