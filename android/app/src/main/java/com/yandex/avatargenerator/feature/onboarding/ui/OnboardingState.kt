package com.yandex.avatargenerator.feature.onboarding.ui

import com.yandex.avatargenerator.core.mvi.BaseState

internal data class OnboardingState(
    val slides: List<OnboardingSlide> = OnboardingSlide.entries,
    val currentPage: Int = 0,
) : BaseState {
    val isLastPage: Boolean get() = currentPage == slides.lastIndex
}
