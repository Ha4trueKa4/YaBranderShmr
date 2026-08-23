package com.yandex.avatargenerator.feature.onboarding.ui

import com.yandex.avatargenerator.feature.onboarding.domain.CompleteOnboardingUseCase
import com.yandex.avatargenerator.feature.onboarding.domain.HasSeenOnboardingUseCase
import com.yandex.avatargenerator.feature.onboarding.fake.FakeOnboardingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Интенты обрабатываются в корутине `BaseViewModel`, поэтому после каждой пачки
 * нужно прокрутить диспетчер (`advanceUntilIdle`) — иначе state ещё старый.
 */
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
    fun `starts on the first of three slides`() {
        val state = viewModel.state.value

        assertEquals(0, state.currentPage)
        assertEquals(3, state.slides.size)
        assertFalse(state.isLastPage)
    }

    @Test
    fun `OnPageChange moves to the requested slide`() = runTest(dispatcher) {
        viewModel.onIntent(OnboardingIntent.OnPageChange(2))
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.currentPage)
        assertTrue(viewModel.state.value.isLastPage)
    }

    @Test
    fun `OnPageChange ignores an index outside the slide range`() = runTest(dispatcher) {
        viewModel.onIntent(OnboardingIntent.OnPageChange(1))
        viewModel.onIntent(OnboardingIntent.OnPageChange(7))
        viewModel.onIntent(OnboardingIntent.OnPageChange(-1))
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.currentPage)
    }

    @Test
    fun `OnPrimaryClick before the last slide only advances the pager`() = runTest(dispatcher) {
        val effects = mutableListOf<OnboardingEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(OnboardingIntent.OnPrimaryClick)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.currentPage)
        // Флаг «онбординг пройден» ставится только в конце: иначе пользователь,
        // закрывший приложение на втором слайде, больше онбординг не увидит.
        assertEquals(0, repository.completedCallCount)
        assertTrue(effects.isEmpty())

        collector.cancel()
    }

    @Test
    fun `OnPrimaryClick on the last slide completes onboarding and navigates home`() =
        runTest(dispatcher) {
            val effects = mutableListOf<OnboardingEffect>()
            val collector = launch { viewModel.effect.toList(effects) }

            viewModel.onIntent(OnboardingIntent.OnPageChange(2))
            viewModel.onIntent(OnboardingIntent.OnPrimaryClick)
            advanceUntilIdle()

            assertEquals(listOf(OnboardingEffect.NavigateToHome), effects)
            assertEquals(1, repository.completedCallCount)
            assertTrue(HasSeenOnboardingUseCase(repository).invoke().first())

            collector.cancel()
        }

    @Test
    fun `OnBackClick steps back one slide and does nothing on the first`() = runTest(dispatcher) {
        viewModel.onIntent(OnboardingIntent.OnPageChange(2))
        viewModel.onIntent(OnboardingIntent.OnBackClick)
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.currentPage)

        viewModel.onIntent(OnboardingIntent.OnBackClick)
        viewModel.onIntent(OnboardingIntent.OnBackClick)
        advanceUntilIdle()
        assertEquals(0, viewModel.state.value.currentPage)
    }
}
