package com.yandex.avatargenerator.core.mvi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private sealed interface TestIntent : BaseIntent {
        data class Add(val value: Int) : TestIntent
        data object Announce : TestIntent
    }

    private data class TestState(val sum: Int = 0, val steps: List<Int> = emptyList()) : BaseState

    private data class TestEffect(val sum: Int) : BaseEffect

    private class TestViewModel : BaseViewModel<TestIntent, TestState, TestEffect>(TestState()) {
        override suspend fun handleIntent(intent: TestIntent, state: TestState) {
            when (intent) {
                // Записываем sum, каким его увидел обработчик: если бы интенты
                // шли параллельно, здесь появилось бы устаревшее значение.
                is TestIntent.Add -> updateState {
                    copy(sum = sum + intent.value, steps = steps + state.sum)
                }

                TestIntent.Announce -> sendEffect(TestEffect(state.sum))
            }
        }
    }

    @Test
    fun `intents are handled one by one, in order, each seeing the previous result`() =
        runTest(dispatcher) {
            val viewModel = TestViewModel()

            viewModel.onIntent(TestIntent.Add(1))
            viewModel.onIntent(TestIntent.Add(2))
            viewModel.onIntent(TestIntent.Add(3))
            advanceUntilIdle()

            assertEquals(6, viewModel.state.value.sum)
            // Каждый обработчик видел сумму, накопленную предыдущими.
            assertEquals(listOf(0, 1, 3), viewModel.state.value.steps)
        }

    @Test
    fun `handleIntent sees the state current for its own intent`() = runTest(dispatcher) {
        val viewModel = TestViewModel()

        val effects = mutableListOf<TestEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(TestIntent.Add(5))
        viewModel.onIntent(TestIntent.Announce)
        advanceUntilIdle()

        assertEquals(listOf(TestEffect(5)), effects)
        collector.cancel()
    }

    @Test
    fun `an effect is delivered exactly once, even to a new subscriber`() = runTest(dispatcher) {
        val viewModel = TestViewModel()

        val first = mutableListOf<TestEffect>()
        val firstCollector = launch { viewModel.effect.toList(first) }

        viewModel.onIntent(TestIntent.Announce)
        advanceUntilIdle()
        firstCollector.cancel()

        // Пересоздание подписки (поворот экрана) не должно переигрывать эффект.
        val second = mutableListOf<TestEffect>()
        val secondCollector = launch { viewModel.effect.toList(second) }
        advanceUntilIdle()

        assertEquals(listOf(TestEffect(0)), first)
        assertEquals(emptyList<TestEffect>(), second)
        secondCollector.cancel()
    }

    @Test
    fun `effects sent before anyone subscribes are buffered, not lost`() = runTest(dispatcher) {
        val viewModel = TestViewModel()

        viewModel.onIntent(TestIntent.Announce)
        advanceUntilIdle()

        val effects = mutableListOf<TestEffect>()
        val collector = launch { viewModel.effect.toList(effects) }
        advanceUntilIdle()

        assertEquals(listOf(TestEffect(0)), effects)
        collector.cancel()
    }
}
