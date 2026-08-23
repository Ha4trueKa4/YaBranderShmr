package com.yandex.avatargenerator.feature.subscription.ui

import com.yandex.avatargenerator.feature.subscription.domain.model.SubscriptionUser
import com.yandex.avatargenerator.feature.subscription.fake.FakeSubscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
internal class SubscriptionsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeSubscriptionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeSubscriptionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(tab: SubscriptionsTab = SubscriptionsTab.Followers) =
        SubscriptionsViewModel(repository, userId = "user-1", tab = tab)

    /** Пользователи текущего Content-состояния — короче, чем каст в каждом тесте. */
    private fun SubscriptionsViewModel.users(): List<SubscriptionUser> =
        (uiState.value.list as SubscriptionsListState.Content).users

    @Test
    fun `loads the initial tab on start`() = runTest(dispatcher) {
        val viewModel = viewModel(SubscriptionsTab.Followers)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SubscriptionsTab.Followers, state.tab)
        assertEquals(SubscriptionsListState.Content(repository.followers), state.list)
    }

    @Test
    fun `opening on the following tab loads the following list`() = runTest(dispatcher) {
        val viewModel = viewModel(SubscriptionsTab.Following)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SubscriptionsTab.Following, state.tab)
        assertEquals(SubscriptionsListState.Content(repository.following), state.list)
    }

    @Test
    fun `a failure shows the error state`() = runTest(dispatcher) {
        repository.listFailure = RuntimeException("boom")
        val viewModel = viewModel(SubscriptionsTab.Followers)
        advanceUntilIdle()

        assertEquals(SubscriptionsListState.Error, viewModel.uiState.value.list)
    }

    @Test
    fun `retry reloads the list after an error`() = runTest(dispatcher) {
        repository.listFailure = RuntimeException("boom")
        val viewModel = viewModel(SubscriptionsTab.Followers)
        advanceUntilIdle()
        assertEquals(SubscriptionsListState.Error, viewModel.uiState.value.list)

        repository.listFailure = null
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(SubscriptionsListState.Content(repository.followers), viewModel.uiState.value.list)
    }

    @Test
    fun `refresh reloads the list and picks up new entries`() = runTest(dispatcher) {
        val viewModel = viewModel(SubscriptionsTab.Following)
        advanceUntilIdle()
        assertEquals(SubscriptionsListState.Content(repository.following), viewModel.uiState.value.list)

        // Пока «ходили в профиль», подписались ещё на одного.
        val newcomer = SubscriptionUser(
            id = "g2",
            displayName = "User g2",
            avatarUrl = null,
            followersCount = 5,
            isFollowing = true,
        )
        repository.following = repository.following + newcomer

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(SubscriptionsListState.Content(repository.following), viewModel.uiState.value.list)
    }

    @Test
    fun `following a user flips the button and bumps the count`() = runTest(dispatcher) {
        val viewModel = viewModel(SubscriptionsTab.Followers)
        advanceUntilIdle()
        val target = viewModel.users().first { !it.isFollowing }

        viewModel.toggleFollow(target)
        advanceUntilIdle()

        val updated = viewModel.users().first { it.id == target.id }
        assertTrue(updated.isFollowing)
        assertEquals(target.followersCount!! + 1, updated.followersCount)
        assertEquals(1, repository.subscribeCallCount)
    }

    @Test
    fun `unfollowing a user flips the button and drops the count`() = runTest(dispatcher) {
        val viewModel = viewModel(SubscriptionsTab.Followers)
        advanceUntilIdle()
        val target = viewModel.users().first { it.isFollowing }

        viewModel.toggleFollow(target)
        advanceUntilIdle()

        val updated = viewModel.users().first { it.id == target.id }
        assertFalse(updated.isFollowing)
        assertEquals(target.followersCount!! - 1, updated.followersCount)
        assertEquals(1, repository.unsubscribeCallCount)
    }

    @Test
    fun `a failed subscribe is rolled back`() = runTest(dispatcher) {
        repository.subscribeFails = true
        val viewModel = viewModel(SubscriptionsTab.Followers)
        advanceUntilIdle()
        val target = viewModel.users().first { !it.isFollowing }

        viewModel.toggleFollow(target)
        advanceUntilIdle()

        val updated = viewModel.users().first { it.id == target.id }
        assertFalse(updated.isFollowing)
        assertEquals(target.followersCount, updated.followersCount)
    }

    @Test
    fun `repeated taps during a running request send a single request`() = runTest(dispatcher) {
        val viewModel = viewModel(SubscriptionsTab.Followers)
        advanceUntilIdle()
        val target = viewModel.users().first { !it.isFollowing }

        // Два тапа подряд, без прокрутки диспетчера между ними: запрос первого
        // ещё не успел завершиться, второй должен быть проигнорирован.
        viewModel.toggleFollow(target)
        viewModel.toggleFollow(target)
        advanceUntilIdle()

        assertEquals(1, repository.subscribeCallCount)
    }

    @Test
    fun `toggling follow on the current user sends no request`() = runTest(dispatcher) {
        val self = repository.followers.first()
        val viewModel = SubscriptionsViewModel(
            repository,
            userId = "user-1",
            tab = SubscriptionsTab.Followers,
            currentUserId = self.id,
        )
        advanceUntilIdle()

        viewModel.toggleFollow(self)
        advanceUntilIdle()

        assertEquals(0, repository.subscribeCallCount)
        assertEquals(0, repository.unsubscribeCallCount)
        val unchanged = viewModel.users().first { it.id == self.id }
        assertEquals(self.isFollowing, unchanged.isFollowing)
    }
}
