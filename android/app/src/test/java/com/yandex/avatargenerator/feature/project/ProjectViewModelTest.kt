package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.Project
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
    private lateinit var projectRepository: FakeProjectRepository
    private lateinit var chatRepository: FakeChatRepository

    private val messages = listOf<ChatMessage>(
        ChatMessage.System.ProjectCreated(id = "system-created"),
        ChatMessage.Outgoing(
            id = "out-1",
            text = "Violet logo, please",
            sentAt = Instant.parse("2026-08-12T11:30:00Z"),
        ),
    )
    private val chat = ProjectChat(id = PROJECT_ID, title = "chat title", messages = messages)
    private val project = Project(
        id = PROJECT_ID,
        authorId = "author-1",
        name = "Orion Brand AI",
        description = null,
        isPublic = true,
        createdAt = null,
        url = null,
        data = null,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        projectRepository = FakeProjectRepository()
        chatRepository = FakeChatRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ProjectViewModel(projectRepository, chatRepository, PROJECT_ID)

    @Test
    fun `starts in loading state`() {
        // StandardTestDispatcher не выполняет корутину загрузки до advanceUntilIdle,
        // поэтому здесь видно именно начальное состояние.
        val viewModel = viewModel()

        assertEquals(ProjectUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `content берёт название и видимость из проекта, а сообщения из чата`() = runTest(dispatcher) {
        projectRepository.projectResult = Result.success(project)
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(
            ProjectUiState.Content(
                title = project.name,
                isPublic = project.isPublic,
                messages = messages,
                draft = "",
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `падение загрузки чата даёт Error`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.failure(IllegalStateException("network is down"))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(ProjectUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun `падение загрузки проекта даёт Error`() = runTest(dispatcher) {
        projectRepository.projectResult = Result.failure(IllegalStateException("404"))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(ProjectUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun `retry после ошибки перезагружает проект и чат`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.failure(IllegalStateException("network is down"))
        val viewModel = viewModel()
        advanceUntilIdle()

        projectRepository.projectResult = Result.success(project)
        chatRepository.chatResult = Result.success(chat)
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(
            ProjectUiState.Content(
                title = project.name,
                isPublic = project.isPublic,
                messages = messages,
                draft = "",
            ),
            viewModel.uiState.value,
        )
        assertEquals(2, chatRepository.getChatCallCount)
        assertEquals(2, projectRepository.getProjectCallCount)
    }

    @Test
    fun `draft change is kept in the state`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDraftChange("Make it rounder")

        assertEquals("Make it rounder", contentOf(viewModel).draft)
    }

    @Test
    fun `send appends the message and clears the draft`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
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
        assertEquals(listOf("Make it rounder"), chatRepository.sentTexts)
    }

    @Test
    fun `send returns the draft back when sending fails`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        chatRepository.sendError = IllegalStateException("network is down")
        val viewModel = viewModel()
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
        chatRepository.chatResult = Result.success(chat)
        chatRepository.sendError = IllegalStateException("network is down")
        val viewModel = viewModel()
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
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDraftChange("   ")
        viewModel.send()
        advanceUntilIdle()

        val content = contentOf(viewModel)
        assertEquals(messages, content.messages)
        assertEquals("   ", content.draft)
        assertEquals(emptyList<String>(), chatRepository.sentTexts)
    }

    private fun contentOf(viewModel: ProjectViewModel): ProjectUiState.Content =
        viewModel.uiState.value as ProjectUiState.Content

    private companion object {
        const val PROJECT_ID = "project-1"
    }
}
