package com.yandex.avatargenerator.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.onboarding.domain.CompleteOnboardingUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class OnboardingViewModel(
    private val completeOnboarding: CompleteOnboardingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effects = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects: Flow<OnboardingEffect> = _effects.receiveAsFlow()

    fun onPageChanged(index: Int) {
        if (index !in _uiState.value.pages.indices) return
        _uiState.update { state -> state.copy(currentPage = index) }
    }

    fun onGetStartedClick() = finishOnboarding(OnboardingEffect.NavigateToHome)

    fun onSignInClick() = finishOnboarding(OnboardingEffect.NavigateToSignIn)

    /**
     * Флаг пишется до отправки эффекта: если процесс убьют сразу после навигации,
     * онбординг не должен показаться повторно.
     */
    private fun finishOnboarding(effect: OnboardingEffect) {
        viewModelScope.launch {
            completeOnboarding()
            _effects.send(effect)
        }
    }
}
