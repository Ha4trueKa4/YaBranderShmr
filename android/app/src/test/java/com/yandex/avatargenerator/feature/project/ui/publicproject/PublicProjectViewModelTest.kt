package com.yandex.avatargenerator.feature.project.ui.publicproject

import com.yandex.avatargenerator.feature.project.data.MockPublicProjectRepository
import com.yandex.avatargenerator.feature.project.domain.model.ExportFormat
import com.yandex.avatargenerator.feature.project.fake.FakeProjectExporter
import com.yandex.avatargenerator.feature.project.fake.FakePublicProjectRepository
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
internal class PublicProjectViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakePublicProjectRepository
    private lateinit var subscriptions: FakeSubscriptionRepository
    private lateinit var exporter: FakeProjectExporter

    private val sample = MockPublicProjectRepository.sample()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakePublicProjectRepository()
        subscriptions = FakeSubscriptionRepository()
        exporter = FakeProjectExporter()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = PublicProjectViewModel(
        repository = repository,
        subscriptionRepository = subscriptions,
        exporter = exporter,
        projectId = "public-sample",
    )

    @Test
    fun `starts in loading state`() {
        val viewModel = viewModel()

        assertTrue(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.project)
    }

    @Test
    fun `loaded project lands in the state`() = runTest(dispatcher) {
        repository.result = Result.success(sample)
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(sample, state.project)
        assertFalse(state.isLoading)
        assertFalse(state.hasError)
    }

    @Test
    fun `failed load surfaces an error and retry reloads`() = runTest(dispatcher) {
        repository.result = Result.failure(IllegalStateException("network is down"))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.hasError)

        repository.result = Result.success(sample)
        viewModel.onIntent(PublicProjectIntent.OnRetryClick)
        advanceUntilIdle()

        assertEquals(sample, viewModel.state.value.project)
        assertFalse(viewModel.state.value.hasError)
        assertEquals(2, repository.callCount)
    }

    @Test
    fun `follow is applied optimistically and kept when the request succeeds`() =
        runTest(dispatcher) {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onIntent(PublicProjectIntent.OnFollowClick)
            advanceUntilIdle()

            assertTrue(viewModel.state.value.project!!.author.isFollowing)
            assertEquals(1, subscriptions.subscribeCallCount)
            assertFalse(viewModel.state.value.isFollowInFlight)
        }

    @Test
    fun `a failed follow request is rolled back`() = runTest(dispatcher) {
        subscriptions.subscribeFails = true
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onIntent(PublicProjectIntent.OnFollowClick)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.project!!.author.isFollowing)
        assertFalse(viewModel.state.value.isFollowInFlight)
    }

    @Test
    fun `a double tap on follow sends a single request`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        // Два тапа подряд, без прокрутки диспетчера между ними: isFollowInFlight
        // должен подняться до launch, иначе второй тап отправил бы unsubscribe.
        viewModel.onIntent(PublicProjectIntent.OnFollowClick)
        viewModel.onIntent(PublicProjectIntent.OnFollowClick)
        advanceUntilIdle()

        assertEquals(1, subscriptions.subscribeCallCount)
        assertEquals(0, subscriptions.unsubscribeCallCount)
    }

    @Test
    fun `export without a preview reports there is nothing to save`() = runTest(dispatcher) {
        repository.result = Result.success(sample.copy(previewUrl = null))
        val viewModel = viewModel()
        advanceUntilIdle()
        val effects = mutableListOf<PublicProjectEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(PublicProjectIntent.OnExportClick(ExportFormat.PNG))
        advanceUntilIdle()

        assertEquals(listOf(PublicProjectEffect.ShowNothingToExport), effects)
        assertEquals(0, exporter.callCount)
        collector.cancel()
    }

    @Test
    fun `successful export reports the format that was saved`() = runTest(dispatcher) {
        repository.result = Result.success(sample.copy(previewUrl = "https://s3/preview.svg"))
        val viewModel = viewModel()
        advanceUntilIdle()
        val effects = mutableListOf<PublicProjectEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(PublicProjectIntent.OnExportClick(ExportFormat.SVG))
        advanceUntilIdle()

        assertEquals(listOf(PublicProjectEffect.ShowExportSuccess(ExportFormat.SVG)), effects)
        assertEquals("https://s3/preview.svg", exporter.lastSvgUrl)
        assertFalse(viewModel.state.value.isExporting)
        collector.cancel()
    }

    @Test
    fun `a failed export is reported and lets the user retry`() = runTest(dispatcher) {
        repository.result = Result.success(sample.copy(previewUrl = "https://s3/preview.svg"))
        exporter.result = Result.failure(IllegalStateException("no disk space"))
        val viewModel = viewModel()
        advanceUntilIdle()
        val effects = mutableListOf<PublicProjectEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(PublicProjectIntent.OnExportClick(ExportFormat.PNG))
        advanceUntilIdle()

        assertEquals(listOf(PublicProjectEffect.ShowExportFailure(ExportFormat.PNG)), effects)
        assertFalse(viewModel.state.value.isExporting)
        collector.cancel()
    }

    @Test
    fun `navigation clicks are one-shot effects, not state`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        val effects = mutableListOf<PublicProjectEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(PublicProjectIntent.OnAuthorClick)
        viewModel.onIntent(PublicProjectIntent.OnRelatedClick("p2"))
        viewModel.onIntent(PublicProjectIntent.OnBackClick)
        advanceUntilIdle()

        assertEquals(
            listOf(
                PublicProjectEffect.OpenAuthor(sample.author.id),
                PublicProjectEffect.OpenRelated("p2"),
                PublicProjectEffect.NavigateBack,
            ),
            effects,
        )
        collector.cancel()
    }
}
