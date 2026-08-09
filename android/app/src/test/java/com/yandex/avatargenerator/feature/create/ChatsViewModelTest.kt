package com.yandex.avatargenerator.feature.create

import com.yandex.avatargenerator.feature.create.domain.model.Chat
import com.yandex.avatargenerator.feature.create.ui.ChatsUiState
import com.yandex.avatargenerator.feature.create.ui.ChatsViewModel
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
internal class ChatsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeChatsRepository

    private val chats = listOf(
        Chat(id = "chat-1", title = "Chat 1", updatedAt = Instant.parse("2026-08-08T12:00:00Z")),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeChatsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts in loading state`() {
        // StandardTestDispatcher не выполняет корутину загрузки до advanceUntilIdle,
        // поэтому здесь видно именно начальное состояние.
        val viewModel = ChatsViewModel(repository)

        assertEquals(ChatsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `non-empty list gives content with the same chats`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = ChatsViewModel(repository)

        advanceUntilIdle()

        assertEquals(ChatsUiState.Content(chats), viewModel.uiState.value)
    }

    @Test
    fun `empty list gives empty state`() = runTest(dispatcher) {
        repository.result = Result.success(emptyList())
        val viewModel = ChatsViewModel(repository)

        advanceUntilIdle()

        assertEquals(ChatsUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `repository failure gives error state`() = runTest(dispatcher) {
        repository.result = Result.failure(IllegalStateException("network is down"))
        val viewModel = ChatsViewModel(repository)

        advanceUntilIdle()

        assertEquals(ChatsUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun `retry after an error reloads the data`() = runTest(dispatcher) {
        repository.result = Result.failure(IllegalStateException("network is down"))
        val viewModel = ChatsViewModel(repository)
        advanceUntilIdle()

        repository.result = Result.success(chats)
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(ChatsUiState.Content(chats), viewModel.uiState.value)
        assertEquals(2, repository.callCount)
    }
}
