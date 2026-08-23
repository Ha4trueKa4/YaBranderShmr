package com.yandex.avatargenerator.feature.create.ui

import com.yandex.avatargenerator.feature.create.domain.model.Chat
import com.yandex.avatargenerator.feature.create.fake.FakeChatsRepository
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
    fun `refresh reloads the list without flashing the skeleton`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = ChatsViewModel(repository)
        advanceUntilIdle()

        val updated = chats + Chat(
            id = "chat-2",
            title = "Chat 2",
            updatedAt = Instant.parse("2026-08-09T12:00:00Z"),
        )
        repository.result = Result.success(updated)
        viewModel.refresh()
        // Список ещё старый, но это Content, а не Loading: скелетон не мигает.
        assertEquals(ChatsUiState.Content(chats), viewModel.uiState.value)

        advanceUntilIdle()

        assertEquals(ChatsUiState.Content(updated), viewModel.uiState.value)
        assertEquals(2, repository.callCount)
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
