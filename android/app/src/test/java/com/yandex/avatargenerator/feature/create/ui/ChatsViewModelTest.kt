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

    // Фейковые часы: троттлинг refresh проверяется сдвигом времени, без задержек.
    private var now: Long = 0L

    private fun viewModel() = ChatsViewModel(repository) { now }

    private val chats = listOf(
        Chat(id = "chat-1", title = "Chat 1", updatedAt = Instant.parse("2026-08-08T12:00:00Z")),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeChatsRepository()
        now = 0L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts in loading state`() {
        // StandardTestDispatcher не выполняет корутину загрузки до advanceUntilIdle,
        // поэтому здесь видно именно начальное состояние.
        val viewModel = viewModel()

        assertEquals(ChatsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `non-empty list gives content with the same chats`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(ChatsUiState.Content(chats), viewModel.uiState.value)
    }

    @Test
    fun `empty list gives empty state`() = runTest(dispatcher) {
        repository.result = Result.success(emptyList())
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(ChatsUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `repository failure gives error state`() = runTest(dispatcher) {
        repository.result = Result.failure(IllegalStateException("network is down"))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(ChatsUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun `refresh reloads the list without flashing the skeleton`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()
        advanceUntilIdle()

        val updated = chats + Chat(
            id = "chat-2",
            title = "Chat 2",
            updatedAt = Instant.parse("2026-08-09T12:00:00Z"),
        )
        repository.result = Result.success(updated)
        now += 11_000
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
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.result = Result.success(chats)
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(ChatsUiState.Content(chats), viewModel.uiState.value)
        assertEquals(2, repository.callCount)
    }

    @Test
    fun `background refresh failure keeps the list and raises the flag`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.result = Result.failure(IllegalStateException("network is down"))
        now += 11_000
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(ChatsUiState.Content(chats, isRefreshFailed = true), viewModel.uiState.value)
    }

    @Test
    fun `successful refresh clears the refresh error flag`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.result = Result.failure(IllegalStateException("network is down"))
        now += 11_000
        viewModel.refresh()
        advanceUntilIdle()

        val updated = chats + Chat(
            id = "chat-2",
            title = "Chat 2",
            updatedAt = Instant.parse("2026-08-09T12:00:00Z"),
        )
        repository.result = Result.success(updated)
        now += 11_000
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(ChatsUiState.Content(updated), viewModel.uiState.value)
    }

    @Test
    fun `refresh right after the load is throttled`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(1, repository.callCount)
    }

    @Test
    fun `refresh after the throttle window reloads`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()
        advanceUntilIdle()

        now += 11_000
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, repository.callCount)
    }

    @Test
    fun `retry ignores the throttle`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, repository.callCount)
    }
}
