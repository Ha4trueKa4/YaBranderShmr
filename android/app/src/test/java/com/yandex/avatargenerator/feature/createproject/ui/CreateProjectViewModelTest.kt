package com.yandex.avatargenerator.feature.createproject.ui

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.feature.project.FakeProjectRepository
import com.yandex.avatargenerator.feature.project.domain.model.ProjectCreation
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
    fun `submit is disabled while the prompt is blank`() = runTest(dispatcher) {
        val viewModel = CreateProjectViewModel(repository)

        assertFalse(viewModel.state.value.canSubmit)

        // Интенты обрабатываются в корутине BaseViewModel, поэтому после каждой
        // пачки нужно прокрутить диспетчер — иначе state ещё старый.
        viewModel.onIntent(CreateProjectIntent.OnPromptChange("   "))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.canSubmit)

        // Одно название кнопку не включает — бэкенду оно больше не нужно.
        viewModel.onIntent(CreateProjectIntent.OnNameChange("Orion Labs"))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.onIntent(CreateProjectIntent.OnPromptChange("Minimal fox mascot"))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `submit with a blank prompt does not create a project`() = runTest(dispatcher) {
        val viewModel = CreateProjectViewModel(repository)

        viewModel.onIntent(CreateProjectIntent.OnSubmitClick)
        advanceUntilIdle()

        assertEquals(0, repository.createProjectCallCount)
    }

    @Test
    fun `submit creates a project and emits navigation effect with its id`() = runTest(dispatcher) {
        repository.createResult = Result.success(
            ProjectCreation(FakeProjectRepository.DEFAULT_PROJECT.copy(id = "project-42"), "job-1"),
        )
        val viewModel = CreateProjectViewModel(repository)
        viewModel.onIntent(CreateProjectIntent.OnPromptChange("  Minimal fox mascot  "))
        viewModel.onIntent(CreateProjectIntent.OnNameChange("  Orion Labs  "))
        viewModel.onIntent(CreateProjectIntent.OnDescriptionChange("  Deep space violet  "))

        val effects = mutableListOf<CreateProjectEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(CreateProjectIntent.OnSubmitClick)
        advanceUntilIdle()

        assertEquals(1, repository.createProjectCallCount)
        // Промпт, название и описание доходят до репозитория обрезанными.
        assertEquals("Minimal fox mascot", repository.lastCreatedPrompt)
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
            viewModel.onIntent(CreateProjectIntent.OnPromptChange("Minimal fox mascot"))
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
            assertEquals("Minimal fox mascot", state.prompt)
            assertEquals("Orion Labs", state.name)
            assertEquals("Deep space violet", state.description)

            // Ошибку сбрасывает правка промпта — именно его отвергает сервер.
            viewModel.onIntent(CreateProjectIntent.OnNameChange("Orion Labs 2"))
            advanceUntilIdle()
            assertTrue(viewModel.state.value.hasError)

            viewModel.onIntent(CreateProjectIntent.OnPromptChange("Minimal owl mascot"))
            advanceUntilIdle()
            assertFalse(viewModel.state.value.hasError)

            collector.cancel()
        }

    @Test
    fun `a double tap creates a single project`() = runTest(dispatcher) {
        val viewModel = CreateProjectViewModel(repository)
        viewModel.onIntent(CreateProjectIntent.OnPromptChange("Minimal fox mascot"))
        advanceUntilIdle()

        // Два тапа подряд, без прокрутки диспетчера между ними: isSubmitting должен
        // подняться до launch, иначе второй тап увидел бы false и создал дубль.
        viewModel.onIntent(CreateProjectIntent.OnSubmitClick)
        viewModel.onIntent(CreateProjectIntent.OnSubmitClick)
        advanceUntilIdle()

        assertEquals(1, repository.createProjectCallCount)
    }

    @Test
    fun `the prompt reaches the repository trimmed`() = runTest(dispatcher) {
        val viewModel = CreateProjectViewModel(repository)
        viewModel.onIntent(
            CreateProjectIntent.OnPromptChange("\n  Minimal fox mascot, flat vector  \n"),
        )

        viewModel.onIntent(CreateProjectIntent.OnSubmitClick)
        advanceUntilIdle()

        assertEquals("Minimal fox mascot, flat vector", repository.lastCreatedPrompt)
    }

    @Test
    fun `a blank name is replaced with the default one`() = runTest(dispatcher) {
        val viewModel = CreateProjectViewModel(repository)
        viewModel.onIntent(CreateProjectIntent.OnPromptChange("Minimal fox mascot"))
        viewModel.onIntent(CreateProjectIntent.OnNameChange("   "))

        viewModel.onIntent(CreateProjectIntent.OnSubmitClick)
        advanceUntilIdle()

        // Иначе проект уехал бы на сервер с пустым именем и встал безымянным
        // и в списке чатов, и в шапке своего экрана.
        assertEquals("Untitled project", repository.lastCreatedName)
    }

    @Test
    fun `a prompt longer than the limit is trimmed in the state`() = runTest(dispatcher) {
        val viewModel = CreateProjectViewModel(repository)

        viewModel.onIntent(
            CreateProjectIntent.OnPromptChange("a".repeat(MaxCreatePromptLength + 100)),
        )
        advanceUntilIdle()

        assertEquals(MaxCreatePromptLength, viewModel.state.value.prompt.length)
    }

    @Test
    fun `a surrogate pair is not cut in half`() = runTest(dispatcher) {
        val viewModel = CreateProjectViewModel(repository)

        // Эмодзи — две UTF-16 char-ы на одну кодовую точку: обрезка по char-ам
        // оставила бы половинку суррогатной пары и отправила её на сервер.
        viewModel.onIntent(
            CreateProjectIntent.OnPromptChange(Fox.repeat(MaxCreatePromptLength + 10)),
        )
        advanceUntilIdle()

        val prompt = viewModel.state.value.prompt
        assertEquals(Fox.repeat(MaxCreatePromptLength), prompt)
        assertEquals(MaxCreatePromptLength, prompt.codePointCount(0, prompt.length))
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

    private companion object {
        /** Эмодзи из суррогатной пары: две UTF-16 char-ы на одну кодовую точку. */
        const val Fox = "\uD83E\uDD8A"
    }
}
