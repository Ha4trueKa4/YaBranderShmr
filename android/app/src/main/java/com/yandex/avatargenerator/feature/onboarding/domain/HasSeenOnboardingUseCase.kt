package com.yandex.avatargenerator.feature.onboarding.domain

import kotlinx.coroutines.flow.Flow

internal class HasSeenOnboardingUseCase(
    private val repository: OnboardingRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.hasSeenOnboarding
}
