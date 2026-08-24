package com.yandex.avatargenerator.feature.profile.ui

import com.yandex.avatargenerator.feature.profile.fake.FakeProfileRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class ProfileViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeProfileRepository
    private lateinit var subscriptions: FakeSubscriptionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeProfileRepository()
        subscriptions = FakeSubscriptionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(isOwnProfile: Boolean = false) = ProfileViewModel(
        repository = repository,
        subscriptionRepository = subscriptions,
        userId = "user-1",
        isOwnProfile = isOwnProfile,
    )

    @Test
    fun `loaded profile lands in the state`() = runTest(dispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(FakeProfileRepository.profile(), state.profile)
        assertFalse(state.isLoading)
        assertFalse(state.hasError)
    }

    @Test
    fun `failed load surfaces an error and retry reloads`() = runTest(dispatcher) {
        repository.result = Result.failure(IllegalStateException("network is down"))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.hasError)

        repository.result = Result.success(FakeProfileRepository.details())
        viewModel.onIntent(ProfileIntent.OnRetryClick)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.hasError)
        assertEquals(2, repository.callCount)
    }

    @Test
    fun `the header button on your own profile opens editing, not a subscription`() =
        runTest(dispatcher) {
            val viewModel = viewModel(isOwnProfile = true)
            advanceUntilIdle()
            val effects = mutableListOf<ProfileEffect>()
            val collector = launch { viewModel.effect.toList(effects) }

            viewModel.onIntent(ProfileIntent.OnActionClick)
            advanceUntilIdle()

            assertEquals(listOf(ProfileEffect.OpenEditProfile), effects)
            assertEquals(0, subscriptions.subscribeCallCount)
            collector.cancel()
        }

    @Test
    fun `follow on someone else's profile updates the flag and the counter at once`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onIntent(ProfileIntent.OnActionClick)
            advanceUntilIdle()

            val profile = viewModel.state.value.profile!!
            assertTrue(profile.isFollowing)
            assertEquals(101, profile.followersCount)
            assertEquals(1, subscriptions.subscribeCallCount)
            assertFalse(viewModel.state.value.isFollowInFlight)
        }

    @Test
    fun `a failed follow request rolls back both the flag and the counter`() =
        runTest(dispatcher) {
            subscriptions.subscribeFails = true
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onIntent(ProfileIntent.OnActionClick)
            advanceUntilIdle()

            val profile = viewModel.state.value.profile!!
            assertFalse(profile.isFollowing)
            assertEquals(100, profile.followersCount)
        }

    @Test
    fun `a double tap on follow sends a single request`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        // Два тапа подряд, без прокрутки диспетчера между ними: isFollowInFlight
        // должен подняться до launch, иначе второй тап отправил бы unsubscribe.
        viewModel.onIntent(ProfileIntent.OnActionClick)
        viewModel.onIntent(ProfileIntent.OnActionClick)
        advanceUntilIdle()

        assertEquals(1, subscriptions.subscribeCallCount)
        assertEquals(0, subscriptions.unsubscribeCallCount)
    }

    @Test
    fun `navigation clicks are one-shot effects, not state`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val effects = mutableListOf<ProfileEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(ProfileIntent.OnProjectClick("project-1"))
        viewModel.onIntent(ProfileIntent.OnFollowersClick)
        viewModel.onIntent(ProfileIntent.OnFollowingClick)
        viewModel.onIntent(ProfileIntent.OnSettingsClick)
        viewModel.onIntent(ProfileIntent.OnBackClick)
        advanceUntilIdle()

        assertEquals(
            listOf(
                ProfileEffect.OpenProject("project-1"),
                // В списки уходит id открытого профиля, а не текущего пользователя.
                ProfileEffect.OpenFollowers("user-1"),
                ProfileEffect.OpenFollowing("user-1"),
                ProfileEffect.OpenSettings,
                ProfileEffect.NavigateBack,
            ),
            effects,
        )
        collector.cancel()
    }
}
