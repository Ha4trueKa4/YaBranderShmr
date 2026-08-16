package com.yandex.avatargenerator.feature.createproject

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.feature.createproject.ui.CreateProjectEvent
import com.yandex.avatargenerator.feature.createproject.ui.CreateProjectViewModel
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
    fun `submit is disabled while the name is blank`() {
        val viewModel = CreateProjectViewModel(repository)

        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onNameChange("   ")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onNameChange("Orion Labs")
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `submit with a blank name does not create a project`() = runTest(dispatcher) {
        val viewModel = CreateProjectViewModel(repository)

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(0, repository.createProjectCallCount)
    }

    @Test
    fun `submit creates a project and emits navigation event with its id`() = runTest(dispatcher) {
        repository.createResult = Result.success(
            FakeProjectRepository.DEFAULT_PROJECT.copy(id = "project-42"),
        )
        val viewModel = CreateProjectViewModel(repository)
        viewModel.onNameChange("  Orion Labs  ")
        viewModel.onDescriptionChange("  Deep space violet  ")

        val events = mutableListOf<CreateProjectEvent>()
        val collector = launch { viewModel.events.toList(events) }

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(1, repository.createProjectCallCount)
        // Название и описание доходят до репозитория обрезанными.
        assertEquals("Orion Labs", repository.lastCreatedName)
        assertEquals("Deep space violet", repository.lastCreatedDescription)
        // В навигацию уходит id из ответа сервера, а не то, что ввёл пользователь.
        assertEquals(listOf(CreateProjectEvent.ProjectCreated("project-42")), events)
        assertFalse(viewModel.uiState.value.isSubmitting)

        collector.cancel()
    }

    @Test
    fun `failed creation surfaces error, re-enables submit and emits no event`() =
        runTest(dispatcher) {
            repository.createResult = Result.failure(NetworkException.NoConnection())
            val viewModel = CreateProjectViewModel(repository)
            viewModel.onNameChange("Orion Labs")
            viewModel.onDescriptionChange("Deep space violet")

            val events = mutableListOf<CreateProjectEvent>()
            val collector = launch { viewModel.events.toList(events) }

            viewModel.onSubmit()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.hasError)
            assertFalse(state.isSubmitting)
            assertTrue(state.canSubmit)
            assertTrue(events.isEmpty())
            // Введённое пользователем не теряется — повтор не заставит набирать заново.
            assertEquals("Orion Labs", state.name)
            assertEquals("Deep space violet", state.description)

            // Правка имени сбрасывает ошибку.
            viewModel.onNameChange("Orion Labs 2")
            assertFalse(viewModel.uiState.value.hasError)

            collector.cancel()
        }
}
