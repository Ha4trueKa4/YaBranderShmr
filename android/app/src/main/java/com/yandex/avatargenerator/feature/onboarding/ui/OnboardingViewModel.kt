package com.yandex.avatargenerator.feature.onboarding.ui

import com.yandex.avatargenerator.core.mvi.BaseViewModel
import com.yandex.avatargenerator.feature.onboarding.domain.CompleteOnboardingUseCase

internal class OnboardingViewModel(
    private val completeOnboarding: CompleteOnboardingUseCase,
) : BaseViewModel<OnboardingIntent, OnboardingState, OnboardingEffect>(OnboardingState()) {

    override suspend fun handleIntent(intent: OnboardingIntent, state: OnboardingState) {
        when (intent) {
            is OnboardingIntent.OnPageChange -> {
                if (intent.index in state.slides.indices) {
                    updateState { copy(currentPage = intent.index) }
                }
            }

            OnboardingIntent.OnBackClick -> {
                if (state.currentPage > 0) {
                    updateState { copy(currentPage = currentPage - 1) }
                }
            }

            OnboardingIntent.OnPrimaryClick -> {
                if (state.isLastPage) {
                    // Флаг пишем до эффекта: убьют процесс сразу после навигации —
                    // онбординг не должен показаться повторно.
                    completeOnboarding()
                    sendEffect(OnboardingEffect.NavigateToHome)
                } else {
                    updateState { copy(currentPage = currentPage + 1) }
                }
            }
        }
    }
}
