package com.yandex.avatargenerator.feature.createproject.ui

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.feature.project.FakeProjectRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CreateProjectViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeProjectRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeProjectRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submit is disabled while the name is blank`() = runTest(dispatcher) {
        val viewModel = CreateProjectViewModel(repository)

        assertFalse(viewModel.state.value.canSubmit)

        // Интенты обрабатываются в корутине BaseViewModel, поэтому после каждой
        // пачки нужно прокрутить диспетчер — иначе state ещё старый.
        viewModel.onIntent(CreateProjectIntent.OnNameChange("   "))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.onIntent(CreateProjectIntent.OnNameChange("Orion Labs"))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `submit with a blank name does not create a project`() = runTest(dispatcher) {
        val viewModel = CreateProjectViewModel(repository)

        viewModel.onIntent(CreateProjectIntent.OnSubmitClick)
        advanceUntilIdle()

        assertEquals(0, repository.createProjectCallCount)
    }

    @Test
    fun `submit creates a project and emits navigation effect with its id`() = runTest(dispatcher) {
        repository.createResult = Result.success(
            FakeProjectRepository.DEFAULT_PROJECT.copy(id = "project-42"),
        )
        val viewModel = CreateProjectViewModel(repository)
        viewModel.onIntent(CreateProjectIntent.OnNameChange("  Orion Labs  "))
        viewModel.onIntent(CreateProjectIntent.OnDescriptionChange("  Deep space violet  "))

        val effects = mutableListOf<CreateProjectEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(CreateProjectIntent.OnSubmitClick)
        advanceUntilIdle()

        assertEquals(1, repository.createProjectCallCount)
        // Название и описание доходят до репозитория обрезанными.
        assertEquals("Orion Labs", repository.lastCreatedName)
        assertEquals("Deep space violet", repository.lastCreatedDescription)
        // В навигацию уходит id из ответа сервера, а не то, что ввёл пользователь.
        assertEquals(listOf(CreateProjectEffect.NavigateToProject("project-42")), effects)
        assertFalse(viewModel.state.value.isSubmitting)

        collector.cancel()
    }

    @Test
    fun `failed creation surfaces error, re-enables submit and emits no effect`() =
        runTest(dispatcher) {
            repository.createResult = Result.failure(NetworkException.NoConnection())
            val viewModel = CreateProjectViewModel(repository)
            viewModel.onIntent(CreateProjectIntent.OnNameChange("Orion Labs"))
            viewModel.onIntent(CreateProjectIntent.OnDescriptionChange("Deep space violet"))

            val effects = mutableListOf<CreateProjectEffect>()
            val collector = launch { viewModel.effect.toList(effects) }

            viewModel.onIntent(CreateProjectIntent.OnSubmitClick)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertTrue(state.hasError)
            assertFalse(state.isSubmitting)
            assertTrue(state.canSubmit)
            assertTrue(effects.isEmpty())
            // Введённое пользователем не теряется — повтор не заставит набирать заново.
            assertEquals("Orion Labs", state.name)
            assertEquals("Deep space violet", state.description)

            // Правка имени сбрасывает ошибку.
            viewModel.onIntent(CreateProjectIntent.OnNameChange("Orion Labs 2"))
            advanceUntilIdle()
            assertFalse(viewModel.state.value.hasError)

            collector.cancel()
        }

    @Test
    fun `a double tap creates a single project`() = runTest(dispatcher) {
        val viewModel = CreateProjectViewModel(repository)
        viewModel.onIntent(CreateProjectIntent.OnNameChange("Orion Labs"))
        advanceUntilIdle()

        // Два тапа подряд, без прокрутки диспетчера между ними: isSubmitting должен
        // подняться до launch, иначе второй тап увидел бы false и создал дубль.
        viewModel.onIntent(CreateProjectIntent.OnSubmitClick)
        viewModel.onIntent(CreateProjectIntent.OnSubmitClick)
        advanceUntilIdle()

        assertEquals(1, repository.createProjectCallCount)
    }

    @Test
    fun `back is a one-shot effect, not state`() = runTest(dispatcher) {
        val viewModel = CreateProjectViewModel(repository)
        val effects = mutableListOf<CreateProjectEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(CreateProjectIntent.OnBackClick)
        advanceUntilIdle()

        assertEquals(listOf(CreateProjectEffect.NavigateBack), effects)
        collector.cancel()
    }
}
