package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat
import com.yandex.avatargenerator.feature.project.ui.MaxPromptBytes
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
    private lateinit var chatRepository: FakeChatRepository
    private lateinit var versionsRepository: FakeVersionsRepository

    private val messages = listOf<ChatMessage>(
        ChatMessage.System.ProjectCreated(id = "system-created"),
        ChatMessage.Outgoing(
            id = "out-1",
            text = "Violet logo, please",
            sentAt = Instant.parse("2026-08-12T11:30:00Z"),
        ),
    )

    /** Лента, какой её отдаёт сервер после удачной генерации. */
    private val reloadedMessages = messages + listOf(
        ChatMessage.Outgoing(
            id = "chat-1-user",
            text = "Make it rounder",
            sentAt = Instant.parse("2026-08-12T11:32:00Z"),
        ),
        ChatMessage.Incoming(
            id = "chat-2-assistant",
            versionId = "v-2",
            sentAt = Instant.parse("2026-08-12T11:32:20Z"),
        ),
    )
    /**
     * Лента с двумя ответами ИИ: сервер прикладывает ссылку только к последней
     * версии, у прошлой её нет — именно её и должен догрузить экран.
     */
    private val twoAnswers = listOf<ChatMessage>(
        ChatMessage.System.ProjectCreated(id = "system-created"),
        ChatMessage.Incoming(
            id = "chat-1-assistant",
            versionId = "v-1",
            sentAt = Instant.parse("2026-08-12T11:30:00Z"),
        ),
        ChatMessage.Incoming(
            id = "chat-3-assistant",
            versionId = "v-2",
            sentAt = Instant.parse("2026-08-12T11:32:00Z"),
            previewUrl = LATEST_URL,
        ),
    )

    private val chat = ProjectChat(
        id = PROJECT_ID,
        title = "Orion Brand AI",
        isPublic = true,
        messages = messages,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        chatRepository = FakeChatRepository()
        versionsRepository = FakeVersionsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ProjectViewModel(chatRepository, versionsRepository, PROJECT_ID)

    @Test
    fun `starts in loading state`() {
        // StandardTestDispatcher не выполняет корутину загрузки до advanceUntilIdle,
        // поэтому здесь видно именно начальное состояние.
        val viewModel = viewModel()

        assertEquals(ProjectUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `content берёт название, видимость и сообщения из ответа репозитория`() =
        runTest(dispatcher) {
            chatRepository.chatResult = Result.success(chat)
            val viewModel = viewModel()

            advanceUntilIdle()

            assertEquals(
                ProjectUiState.Content(
                    title = chat.title,
                    isPublic = chat.isPublic,
                    messages = messages,
                    draft = "",
                ),
                viewModel.uiState.value,
            )
        }

    @Test
    fun `падение загрузки даёт Error`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.failure(IllegalStateException("network is down"))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(ProjectUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun `пустая лента — это Content, а не Error`() = runTest(dispatcher) {
        // Новый проект и недоступная история у не-автора выглядят одинаково:
        // экран открывается, просто показывать в ленте нечего.
        chatRepository.chatResult = Result.success(chat.copy(messages = emptyList()))
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(emptyList<ChatMessage>(), contentOf(viewModel).messages)
    }

    @Test
    fun `retry после ошибки перезагружает чат`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.failure(IllegalStateException("network is down"))
        val viewModel = viewModel()
        advanceUntilIdle()

        chatRepository.chatResult = Result.success(chat)
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(
            ProjectUiState.Content(
                title = chat.title,
                isPublic = chat.isPublic,
                messages = messages,
                draft = "",
            ),
            viewModel.uiState.value,
        )
        assertEquals(2, chatRepository.getChatCallCount)
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
    fun `промпт появляется в ленте сразу, до ответа сервера`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        // По краям пробелы: до репозитория должен дойти обрезанный текст.
        viewModel.onDraftChange("  Make it rounder  ")
        viewModel.send()
        // advanceUntilIdle нет намеренно: генерация ещё идёт.

        val content = contentOf(viewModel)
        assertEquals("", content.draft)
        assertEquals(true, content.isSending)
        assertEquals(
            listOf("Make it rounder"),
            content.messages.filterIsInstance<ChatMessage.Outgoing>()
                .map { it.text } - messages.filterIsInstance<ChatMessage.Outgoing>().map { it.text },
        )
    }

    @Test
    fun `после удачной генерации лента перечитывается, а не дописывается`() =
        runTest(dispatcher) {
            chatRepository.chatResult = Result.success(chat)
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onDraftChange("Make it rounder")
            // Сервер сам дописал пару «промпт → версия» — на перезапросе придёт
            // уже она, с серверными ключами.
            chatRepository.chatResult = Result.success(chat.copy(messages = reloadedMessages))
            viewModel.send()
            advanceUntilIdle()

            val content = contentOf(viewModel)
            // Оптимистичное сообщение не осталось рядом с серверным.
            assertEquals(reloadedMessages, content.messages)
            assertEquals(false, content.isSending)
            assertEquals(listOf("Make it rounder"), chatRepository.sentPrompts)
            assertEquals(2, chatRepository.getChatCallCount)
        }

    @Test
    fun `неудачный перезапрос ленты не превращается в Error`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDraftChange("Make it rounder")
        chatRepository.chatResult = Result.failure(IllegalStateException("network is down"))
        viewModel.send()
        advanceUntilIdle()

        // Версия создана — показывать ошибку было бы враньём. Лента остаётся
        // с оптимистичным сообщением, экран разблокирован.
        val content = contentOf(viewModel)
        assertEquals(messages.size + 1, content.messages.size)
        assertEquals(false, content.isSending)
    }

    @Test
    fun `ошибка генерации возвращает текст и убирает промпт из ленты`() = runTest(dispatcher) {
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
        assertEquals(false, content.isSending)
    }

    @Test
    fun `после ошибки генерации лента не перезапрашивается`() = runTest(dispatcher) {
        // 422 и 503 бэкенд в историю не пишет — перечитывать нечего.
        chatRepository.chatResult = Result.success(chat)
        chatRepository.sendError = IllegalStateException("boom")
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(1, chatRepository.getChatCallCount)
    }

    @Test
    fun `ошибка генерации не перетирает новый черновик`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        chatRepository.sendError = IllegalStateException("network is down")
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        // Генерация ещё идёт (StandardTestDispatcher), пользователь успевает
        // начать новое сообщение — его перетирать нельзя.
        viewModel.onDraftChange("Actually, make it square")
        advanceUntilIdle()

        val content = contentOf(viewModel)
        assertEquals("Actually, make it square", content.draft)
        assertEquals(messages, content.messages)
    }

    @Test
    fun `второй send во время генерации игнорируется`() = runTest(dispatcher) {
        // Иначе одно нажатие «на всякий случай» создало бы лишнюю версию.
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        viewModel.onDraftChange("And bluer")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(listOf("Make it rounder"), chatRepository.sentPrompts)
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
        assertEquals(emptyList<String>(), chatRepository.sentPrompts)
    }

    @Test
    fun `черновик обрезается по лимиту промпта в байтах`() = runTest(dispatcher) {
        // Кириллица — два байта на символ, значит влезает вдвое меньше символов.
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDraftChange("я".repeat(MaxPromptBytes))

        val draft = contentOf(viewModel).draft
        assertEquals(MaxPromptBytes / 2, draft.length)
        assertEquals(MaxPromptBytes, draft.toByteArray().size)
    }

    @Test
    fun `таймаут ждёт версию опросом, а не считается ошибкой`() = runTest(dispatcher) {
        // Сервер доводит генерацию до конца и после обрыва — версия появляется
        // с задержкой, и промпт из ленты выбрасывать нельзя.
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        chatRepository.sendError = NetworkException.Timeout()
        // Первый опрос застаёт ленту прежней (модель ещё считает), второй — уже
        // с серверной парой «промпт → версия».
        chatRepository.chatResults += Result.success(chat)
        chatRepository.chatResults += Result.success(chat.copy(messages = reloadedMessages))
        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        val content = contentOf(viewModel)
        assertEquals(reloadedMessages, content.messages)
        assertEquals("", content.draft)
        assertEquals(false, content.isSending)
        // Загрузка экрана плюс два опроса: как только версия нашлась, опрос встал.
        assertEquals(3, chatRepository.getChatCallCount)
    }

    @Test
    fun `упавший опрос не прерывает ожидание версии`() = runTest(dispatcher) {
        // Сеть могла лечь ровно на этом тике — версия на сервере от этого никуда
        // не делась.
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        chatRepository.sendError = NetworkException.Timeout()
        chatRepository.chatResults += Result.failure(IllegalStateException("network is down"))
        chatRepository.chatResults += Result.success(chat.copy(messages = reloadedMessages))
        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(reloadedMessages, contentOf(viewModel).messages)
    }

    @Test
    fun `если версия так и не появилась, промпт возвращается в поле`() = runTest(dispatcher) {
        // Окно опроса вышло — считаем генерацию неудавшейся и ведём себя как при
        // обычной ошибке.
        chatRepository.chatResult = Result.success(chat)
        chatRepository.sendError = NetworkException.Timeout()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        val content = contentOf(viewModel)
        assertEquals("Make it rounder", content.draft)
        assertEquals(messages, content.messages)
        assertEquals(false, content.isSending)
    }

    @Test
    fun `превью прошлой версии догружается, когда её пузырь дошёл до экрана`() =
        runTest(dispatcher) {
            // Сервер отдаёт ссылку лишь к последней версии — остальные пузыри без
            // догрузки остались бы без картинки.
            chatRepository.chatResult = Result.success(chat.copy(messages = twoAnswers))
            versionsRepository.urls["v-1"] = OLD_URL
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onPreviewNeeded("v-1")
            advanceUntilIdle()

            val previews = contentOf(viewModel).messages
                .filterIsInstance<ChatMessage.Incoming>()
                .associate { it.versionId to it.previewUrl }
            assertEquals(mapOf("v-1" to OLD_URL, "v-2" to LATEST_URL), previews)
        }

    @Test
    fun `без показа пузыря за ссылкой не ходим`() = runTest(dispatcher) {
        // Открытие экрана не должно стоить запроса на каждую версию в истории.
        chatRepository.chatResult = Result.success(chat.copy(messages = twoAnswers))
        versionsRepository.urls["v-1"] = OLD_URL

        viewModel()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), versionsRepository.requestedVersionIds)
    }

    @Test
    fun `повторный показ пузыря не шлёт второй запрос`() = runTest(dispatcher) {
        // Пузырь в LazyColumn пересоздаётся на каждом заходе в зону видимости.
        chatRepository.chatResult = Result.success(chat.copy(messages = twoAnswers))
        versionsRepository.urls["v-1"] = OLD_URL
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPreviewNeeded("v-1")
        advanceUntilIdle()
        viewModel.onPreviewNeeded("v-1")
        advanceUntilIdle()

        assertEquals(listOf("v-1"), versionsRepository.requestedVersionIds)
    }

    @Test
    fun `за готовой ссылкой последней версии второй раз не ходим`() = runTest(dispatcher) {
        // Ссылку к v-2 сервер прислал сам вместе с историей.
        chatRepository.chatResult = Result.success(chat.copy(messages = twoAnswers))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPreviewNeeded("v-2")
        advanceUntilIdle()

        assertEquals(emptyList<String>(), versionsRepository.requestedVersionIds)
    }

    @Test
    fun `упавшая догрузка превью не ломает ленту и не повторяется`() = runTest(dispatcher) {
        // Ссылки для v-1 у репозитория нет — пузырь останется без картинки, но
        // экран должен жить дальше.
        chatRepository.chatResult = Result.success(chat.copy(messages = twoAnswers))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPreviewNeeded("v-1")
        advanceUntilIdle()
        viewModel.onPreviewNeeded("v-1")
        advanceUntilIdle()

        val content = contentOf(viewModel)
        assertEquals(twoAnswers.size, content.messages.size)
        assertEquals(
            null,
            content.messages.filterIsInstance<ChatMessage.Incoming>()
                .first { it.versionId == "v-1" }
                .previewUrl,
        )
        assertEquals(listOf("v-1"), versionsRepository.requestedVersionIds)
    }

    @Test
    fun `перечитывание ленты не теряет уже загруженные превью`() = runTest(dispatcher) {
        // После генерации сервер снова отдаёт ссылку только к последней версии.
        // Без кэша прошлые пузыри мигали бы пустотой на каждом обновлении.
        chatRepository.chatResult = Result.success(chat.copy(messages = twoAnswers))
        versionsRepository.urls["v-1"] = OLD_URL
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onPreviewNeeded("v-1")
        advanceUntilIdle()

        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        val old = contentOf(viewModel).messages
            .filterIsInstance<ChatMessage.Incoming>()
            .first { it.versionId == "v-1" }
        assertEquals(OLD_URL, old.previewUrl)
        // Повторного запроса за той же ссылкой не было — взяли из кэша.
        assertEquals(listOf("v-1"), versionsRepository.requestedVersionIds)
    }

    private fun contentOf(viewModel: ProjectViewModel): ProjectUiState.Content =
        viewModel.uiState.value as ProjectUiState.Content

    private companion object {
        const val PROJECT_ID = "project-1"
        const val LATEST_URL = "https://s3.example/v2.svg"
        const val OLD_URL = "https://s3.example/v1.svg"
    }
}
