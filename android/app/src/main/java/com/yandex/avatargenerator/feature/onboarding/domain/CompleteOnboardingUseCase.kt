package com.yandex.avatargenerator.feature.onboarding.domain

internal class CompleteOnboardingUseCase(
    private val repository: OnboardingRepository,
) {
    suspend operator fun invoke() = repository.setOnboardingCompleted()
}
