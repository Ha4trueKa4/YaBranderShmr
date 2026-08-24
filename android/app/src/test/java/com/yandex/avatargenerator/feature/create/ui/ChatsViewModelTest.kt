package com.yandex.avatargenerator.feature.create.ui

import com.yandex.avatargenerator.feature.create.domain.model.Chat
import com.yandex.avatargenerator.feature.create.fake.FakeChatsRepository
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

    private val updatedChats = chats + Chat(
        id = "chat-2",
        title = "Chat 2",
        updatedAt = Instant.parse("2026-08-09T12:00:00Z"),
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

        assertTrue(viewModel.state.value.isLoading)
    }

    @Test
    fun `non-empty list gives content with the same chats`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(chats, state.chats)
        assertFalse(state.isLoading)
        assertFalse(state.hasError)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `empty list gives empty state, not an error`() = runTest(dispatcher) {
        repository.result = Result.success(emptyList())
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isEmpty)
        assertFalse(state.hasError)
    }

    @Test
    fun `repository failure gives error state`() = runTest(dispatcher) {
        repository.result = Result.failure(IllegalStateException("network is down"))
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.hasError)
        assertFalse(state.isLoading)
        // Пустой список при ошибке не должен читаться как «чатов нет».
        assertFalse(state.isEmpty)
    }

    @Test
    fun `retry after an error reloads the data`() = runTest(dispatcher) {
        repository.result = Result.failure(IllegalStateException("network is down"))
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.result = Result.success(chats)
        viewModel.onIntent(ChatsIntent.OnRetryClick)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(chats, state.chats)
        assertFalse(state.hasError)
        assertEquals(2, repository.callCount)
    }

    @Test
    fun `clicks are one-shot navigation effects, not state`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()
        val effects = mutableListOf<ChatsEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(ChatsIntent.OnChatClick("chat-1"))
        viewModel.onIntent(ChatsIntent.OnNewProjectClick)
        advanceUntilIdle()

        assertEquals(
            listOf(ChatsEffect.OpenChat("chat-1"), ChatsEffect.OpenCreateProject),
            effects,
        )
        collector.cancel()
    }

    @Test
    fun `resume reloads the list without flashing the skeleton`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.result = Result.success(updatedChats)
        now += 11_000
        viewModel.onIntent(ChatsIntent.OnScreenResumed)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(updatedChats, state.chats)
        assertFalse(state.isLoading)
        assertEquals(2, repository.callCount)
    }

    @Test
    fun `background refresh failure keeps the list and raises the flag`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.result = Result.failure(IllegalStateException("network is down"))
        now += 11_000
        viewModel.onIntent(ChatsIntent.OnScreenResumed)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(chats, state.chats)
        assertTrue(state.isRefreshFailed)
        // Список остался — полноэкранная ошибка его не стирает.
        assertFalse(state.hasError)
    }

    @Test
    fun `successful refresh clears the refresh error flag`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.result = Result.failure(IllegalStateException("network is down"))
        now += 11_000
        viewModel.onIntent(ChatsIntent.OnScreenResumed)
        advanceUntilIdle()

        repository.result = Result.success(updatedChats)
        now += 11_000
        viewModel.onIntent(ChatsIntent.OnScreenResumed)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(updatedChats, state.chats)
        assertFalse(state.isRefreshFailed)
    }

    @Test
    fun `resume right after the load is throttled`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(ChatsIntent.OnScreenResumed)
        advanceUntilIdle()

        assertEquals(1, repository.callCount)
    }

    @Test
    fun `resume after the throttle window reloads`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()
        advanceUntilIdle()

        now += 11_000
        viewModel.onIntent(ChatsIntent.OnScreenResumed)
        advanceUntilIdle()

        assertEquals(2, repository.callCount)
    }

    @Test
    fun `retry ignores the throttle`() = runTest(dispatcher) {
        repository.result = Result.success(chats)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(ChatsIntent.OnRetryClick)
        advanceUntilIdle()

        assertEquals(2, repository.callCount)
    }
}
