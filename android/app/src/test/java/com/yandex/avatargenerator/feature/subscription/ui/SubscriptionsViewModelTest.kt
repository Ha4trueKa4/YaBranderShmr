package com.yandex.avatargenerator.feature.subscription.ui

import com.yandex.avatargenerator.feature.subscription.domain.model.SubscriptionUser
import com.yandex.avatargenerator.feature.subscription.fake.FakeSubscriptionRepository
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
import org.junit.Assert.assertNull
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

    /** Пользователи текущего состояния — короче, чем читать поле в каждом тесте. */
    private fun SubscriptionsViewModel.users(): List<SubscriptionUser> = state.value.users

    @Test
    fun `loads the initial tab on start`() = runTest(dispatcher) {
        val viewModel = viewModel(SubscriptionsTab.Followers)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(SubscriptionsTab.Followers, state.tab)
        assertEquals(repository.followers, state.users)
        assertFalse(state.isLoading)
        assertFalse(state.hasError)
    }

    @Test
    fun `opening on the following tab loads the following list`() = runTest(dispatcher) {
        val viewModel = viewModel(SubscriptionsTab.Following)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(SubscriptionsTab.Following, state.tab)
        assertEquals(repository.following, state.users)
    }

    @Test
    fun `a failure shows the error state`() = runTest(dispatcher) {
        repository.listFailure = RuntimeException("boom")
        val viewModel = viewModel(SubscriptionsTab.Followers)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.hasError)
        assertFalse(state.isLoading)
        // Пустой список при ошибке не должен читаться как «подписчиков нет».
        assertFalse(state.isEmpty)
    }

    @Test
    fun `retry reloads the list after an error`() = runTest(dispatcher) {
        repository.listFailure = RuntimeException("boom")
        val viewModel = viewModel(SubscriptionsTab.Followers)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.hasError)

        repository.listFailure = null
        viewModel.onIntent(SubscriptionsIntent.OnRetryClick)
        advanceUntilIdle()

        assertEquals(repository.followers, viewModel.state.value.users)
        assertFalse(viewModel.state.value.hasError)
    }

    @Test
    fun `a resume reloads the list and picks up new entries`() =
        runTest(dispatcher) {
            val viewModel = viewModel(SubscriptionsTab.Following)
            advanceUntilIdle()
            assertEquals(repository.following, viewModel.state.value.users)

            // Пока «ходили в профиль», подписались ещё на одного.
            val newcomer = SubscriptionUser(
                id = "g2",
                displayName = "User g2",
                avatarUrl = null,
                followersCount = 5,
                isFollowing = true,
            )
            repository.following = repository.following + newcomer

            viewModel.onIntent(SubscriptionsIntent.OnScreenResumed)
            advanceUntilIdle()

            assertEquals(repository.following, viewModel.state.value.users)
            assertEquals(2, repository.followingCallCount)
        }

    @Test
    fun `the current user id arrives by intent, after the view model is built`() =
        runTest(dispatcher) {
            // Так и приходит из UI: на первом кадре id ещё не прочитан.
            val viewModel = viewModel(SubscriptionsTab.Followers)
            viewModel.onIntent(SubscriptionsIntent.OnCurrentUserResolved(null))
            advanceUntilIdle()
            assertNull(viewModel.state.value.currentUserId)

            val self = repository.followers.first()
            viewModel.onIntent(SubscriptionsIntent.OnCurrentUserResolved(self.id))
            advanceUntilIdle()

            assertEquals(self.id, viewModel.state.value.currentUserId)
        }

    @Test
    fun `following a user flips the button and bumps the count`() = runTest(dispatcher) {
        val viewModel = viewModel(SubscriptionsTab.Followers)
        advanceUntilIdle()
        val target = viewModel.users().first { !it.isFollowing }

        viewModel.onIntent(SubscriptionsIntent.OnFollowClick(target))
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

        viewModel.onIntent(SubscriptionsIntent.OnFollowClick(target))
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

        viewModel.onIntent(SubscriptionsIntent.OnFollowClick(target))
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
        viewModel.onIntent(SubscriptionsIntent.OnFollowClick(target))
        viewModel.onIntent(SubscriptionsIntent.OnFollowClick(target))
        advanceUntilIdle()

        assertEquals(1, repository.subscribeCallCount)
    }

    @Test
    fun `toggling follow on the current user sends no request`() = runTest(dispatcher) {
        val self = repository.followers.first()
        val viewModel = viewModel(SubscriptionsTab.Followers)
        viewModel.onIntent(SubscriptionsIntent.OnCurrentUserResolved(self.id))
        advanceUntilIdle()

        viewModel.onIntent(SubscriptionsIntent.OnFollowClick(self))
        advanceUntilIdle()

        assertEquals(0, repository.subscribeCallCount)
        assertEquals(0, repository.unsubscribeCallCount)
        val unchanged = viewModel.users().first { it.id == self.id }
        assertEquals(self.isFollowing, unchanged.isFollowing)
    }

    @Test
    fun `tapping your own row goes to your profile tab, not to someone else's`() =
        runTest(dispatcher) {
            val self = repository.followers.first()
            val viewModel = viewModel(SubscriptionsTab.Followers)
            viewModel.onIntent(SubscriptionsIntent.OnCurrentUserResolved(self.id))
            advanceUntilIdle()
            val effects = mutableListOf<SubscriptionsEffect>()
            val collector = launch { viewModel.effect.toList(effects) }

            viewModel.onIntent(SubscriptionsIntent.OnUserClick(self.id))
            viewModel.onIntent(SubscriptionsIntent.OnUserClick("someone-else"))
            viewModel.onIntent(SubscriptionsIntent.OnBackClick)
            advanceUntilIdle()

            assertEquals(
                listOf(
                    SubscriptionsEffect.OpenOwnProfile,
                    SubscriptionsEffect.OpenUserProfile("someone-else"),
                    SubscriptionsEffect.NavigateBack,
                ),
                effects,
            )
            collector.cancel()
        }
}
