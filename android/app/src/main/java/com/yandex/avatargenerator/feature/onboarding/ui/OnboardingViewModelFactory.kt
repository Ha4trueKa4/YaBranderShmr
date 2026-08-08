package com.yandex.avatargenerator.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yandex.avatargenerator.feature.onboarding.domain.CompleteOnboardingUseCase
import com.yandex.avatargenerator.feature.onboarding.domain.OnboardingRepository

/**
 * Ручная фабрика ViewModel (пока без DI) — как [com.yandex.avatargenerator.feature.feed.ui.FeedViewModelFactory].
 * Когда в проект добавят DI, заменится на конструкторную инъекцию.
 */
internal class OnboardingViewModelFactory(
    private val repository: OnboardingRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return OnboardingViewModel(CompleteOnboardingUseCase(repository)) as T
    }
}
