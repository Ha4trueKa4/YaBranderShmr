package com.yandex.avatargenerator.feature.onboarding

import com.yandex.avatargenerator.feature.onboarding.domain.OnboardingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeOnboardingRepository(
    initialHasSeen: Boolean = false,
) : OnboardingRepository {

    private val state = MutableStateFlow(initialHasSeen)

    var completedCallCount: Int = 0
        private set

    override val hasSeenOnboarding: StateFlow<Boolean> = state

    override suspend fun setOnboardingCompleted() {
        completedCallCount++
        state.value = true
    }
}
