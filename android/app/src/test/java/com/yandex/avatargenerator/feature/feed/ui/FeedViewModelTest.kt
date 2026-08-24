package com.yandex.avatargenerator.feature.feed.ui

import com.yandex.avatargenerator.feature.feed.domain.model.FeedTab
import com.yandex.avatargenerator.feature.feed.fake.FakeFeedRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class FeedViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeFeedRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeFeedRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts on the common feed tab`() {
        val viewModel = FeedViewModel(repository)

        assertEquals(FeedTab.ALL, viewModel.state.value.selectedTab)
    }

    @Test
    fun `selecting a tab puts it into the state`() = runTest(dispatcher) {
        val viewModel = FeedViewModel(repository)

        viewModel.onIntent(FeedIntent.OnTabSelected(FeedTab.SUBSCRIPTIONS))
        advanceUntilIdle()

        assertEquals(FeedTab.SUBSCRIPTIONS, viewModel.state.value.selectedTab)
    }

    @Test
    fun `explore from the empty subscriptions feed returns to the common tab`() =
        runTest(dispatcher) {
            val viewModel = FeedViewModel(repository)
            viewModel.onIntent(FeedIntent.OnTabSelected(FeedTab.SUBSCRIPTIONS))
            advanceUntilIdle()

            viewModel.onIntent(FeedIntent.OnExploreClick)
            advanceUntilIdle()

            assertEquals(FeedTab.ALL, viewModel.state.value.selectedTab)
        }

    @Test
    fun `clicks are one-shot navigation effects, not state`() = runTest(dispatcher) {
        val viewModel = FeedViewModel(repository)
        val effects = mutableListOf<FeedEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(FeedIntent.OnProjectClick("project-1"))
        viewModel.onIntent(FeedIntent.OnAuthorClick("author-1"))
        advanceUntilIdle()

        assertEquals(
            listOf(FeedEffect.OpenProject("project-1"), FeedEffect.OpenAuthor("author-1")),
            effects,
        )
        collector.cancel()
    }
}
