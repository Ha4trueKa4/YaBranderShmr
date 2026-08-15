package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat
import com.yandex.avatargenerator.feature.project.ui.ProjectUiState
import com.yandex.avatargenerator.feature.project.ui.ProjectViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class ProjectViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeChatRepository

    private val messages = listOf<ChatMessage>(
        ChatMessage.System.ProjectCreated(id = "system-created"),
        ChatMessage.Outgoing(
            id = "out-1",
            text = "Violet logo, please",
            sentAt = Instant.parse("2026-08-12T11:30:00Z"),
        ),
    )
    private val chat = ProjectChat(id = PROJECT_ID, title = "Orion Brand AI", messages = messages)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeChatRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts in loading state`() {
        // StandardTestDispatcher не выполняет корутину загрузки до advanceUntilIdle,
        // поэтому здесь видно именно начальное состояние.
        val viewModel = ProjectViewModel(repository, PROJECT_ID)

        assertEquals(ProjectUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `loaded chat gives content with its title and messages`() = runTest(dispatcher) {
        repository.chatResult = Result.success(chat)
        val viewModel = ProjectViewModel(repository, PROJECT_ID)

        advanceUntilIdle()

        assertEquals(
            ProjectUiState.Content(title = chat.title, messages = messages, draft = ""),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `repository failure gives error state`() = runTest(dispatcher) {
        repository.chatResult = Result.failure(IllegalStateException("network is down"))
        val viewModel = ProjectViewModel(repository, PROJECT_ID)

        advanceUntilIdle()

        assertEquals(ProjectUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun `retry after an error reloads the chat`() = runTest(dispatcher) {
        repository.chatResult = Result.failure(IllegalStateException("network is down"))
        val viewModel = ProjectViewModel(repository, PROJECT_ID)
        advanceUntilIdle()

        repository.chatResult = Result.success(chat)
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(
            ProjectUiState.Content(title = chat.title, messages = messages, draft = ""),
            viewModel.uiState.value,
        )
        assertEquals(2, repository.getChatCallCount)
    }

    @Test
    fun `draft change is kept in the state`() = runTest(dispatcher) {
        repository.chatResult = Result.success(chat)
        val viewModel = ProjectViewModel(repository, PROJECT_ID)
        advanceUntilIdle()

        viewModel.onDraftChange("Make it rounder")

        assertEquals("Make it rounder", contentOf(viewModel).draft)
    }

    @Test
    fun `send appends the message and clears the draft`() = runTest(dispatcher) {
        repository.chatResult = Result.success(chat)
        val viewModel = ProjectViewModel(repository, PROJECT_ID)
        advanceUntilIdle()

        // По краям пробелы: до репозитория должен дойти обрезанный текст.
        viewModel.onDraftChange("  Make it rounder  ")
        viewModel.send()
        advanceUntilIdle()

        val expected = ChatMessage.Outgoing(
            id = "sent-1",
            text = "Make it rounder",
            sentAt = FakeChatRepository.SENT_AT,
        )
        val content = contentOf(viewModel)
        assertEquals(messages + expected, content.messages)
        assertEquals("", content.draft)
        assertEquals(listOf("Make it rounder"), repository.sentTexts)
    }

    @Test
    fun `send returns the draft back when sending fails`() = runTest(dispatcher) {
        repository.chatResult = Result.success(chat)
        repository.sendError = IllegalStateException("network is down")
        val viewModel = ProjectViewModel(repository, PROJECT_ID)
        advanceUntilIdle()

        viewModel.onDraftChange("  Make it rounder  ")
        viewModel.send()
        advanceUntilIdle()

        val content = contentOf(viewModel)
        // Возвращается уже обрезанный текст — именно тот, что уходил в репозиторий.
        assertEquals("Make it rounder", content.draft)
        assertEquals(messages, content.messages)
    }

    @Test
    fun `send does not overwrite a new draft when sending fails`() = runTest(dispatcher) {
        repository.chatResult = Result.success(chat)
        repository.sendError = IllegalStateException("network is down")
        val viewModel = ProjectViewModel(repository, PROJECT_ID)
        advanceUntilIdle()

        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        // Отправка ещё не завершилась (StandardTestDispatcher), пользователь
        // успевает начать новое сообщение — его перетирать нельзя.
        viewModel.onDraftChange("Actually, make it square")
        advanceUntilIdle()

        val content = contentOf(viewModel)
        assertEquals("Actually, make it square", content.draft)
        assertEquals(messages, content.messages)
    }

    @Test
    fun `send does nothing for a blank draft`() = runTest(dispatcher) {
        repository.chatResult = Result.success(chat)
        val viewModel = ProjectViewModel(repository, PROJECT_ID)
        advanceUntilIdle()

        viewModel.onDraftChange("   ")
        viewModel.send()
        advanceUntilIdle()

        val content = contentOf(viewModel)
        assertEquals(messages, content.messages)
        assertEquals("   ", content.draft)
        assertEquals(emptyList<String>(), repository.sentTexts)
    }

    private fun contentOf(viewModel: ProjectViewModel): ProjectUiState.Content =
        viewModel.uiState.value as ProjectUiState.Content

    private companion object {
        const val PROJECT_ID = "project-1"
    }
}
