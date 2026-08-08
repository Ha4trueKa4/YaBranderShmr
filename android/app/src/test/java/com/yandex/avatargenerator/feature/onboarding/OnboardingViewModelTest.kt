package com.yandex.avatargenerator.feature.onboarding

import com.yandex.avatargenerator.feature.onboarding.domain.CompleteOnboardingUseCase
import com.yandex.avatargenerator.feature.onboarding.domain.HasSeenOnboardingUseCase
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingEffect
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class OnboardingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeOnboardingRepository
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeOnboardingRepository()
        viewModel = OnboardingViewModel(CompleteOnboardingUseCase(repository))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts on the first page`() {
        assertEquals(0, viewModel.uiState.value.currentPage)
        assertEquals(3, viewModel.uiState.value.pages.size)
        assertFalse(viewModel.uiState.value.isLastPage)
    }

    @Test
    fun `onPageChanged moves to the requested page`() {
        viewModel.onPageChanged(2)

        assertEquals(2, viewModel.uiState.value.currentPage)
        assertTrue(viewModel.uiState.value.isLastPage)
    }

    @Test
    fun `onPageChanged ignores an index outside the page range`() {
        viewModel.onPageChanged(1)
        viewModel.onPageChanged(7)
        viewModel.onPageChanged(-1)

        assertEquals(1, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `onGetStartedClick marks onboarding completed and navigates home`() = runTest(dispatcher) {
        viewModel.onGetStartedClick()

        assertEquals(OnboardingEffect.NavigateToHome, viewModel.effects.first())
        assertEquals(1, repository.completedCallCount)
        assertTrue(HasSeenOnboardingUseCase(repository).invoke().first())
    }

    @Test
    fun `onSignInClick marks onboarding completed and navigates to sign in`() = runTest(dispatcher) {
        viewModel.onSignInClick()

        assertEquals(OnboardingEffect.NavigateToSignIn, viewModel.effects.first())
        assertEquals(1, repository.completedCallCount)
        assertTrue(HasSeenOnboardingUseCase(repository).invoke().first())
    }
}
