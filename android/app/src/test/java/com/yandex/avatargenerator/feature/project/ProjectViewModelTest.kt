package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.GenerationOutcome
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
            jobId = FakeChatRepository.JOB_ID,
        ),
    )

    /** Та же лента, но ответ бэкенд ещё не дописал: есть только промпт. */
    private val reloadedWithoutAnswer = messages + listOf(
        ChatMessage.Outgoing(
            id = "chat-1-user",
            text = "Make it rounder",
            sentAt = Instant.parse("2026-08-12T11:32:00Z"),
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
            jobId = FakeChatRepository.JOB_ID,
        ),
    )

    /** Лента, какой её отдаёт сервер после провала: промпт и запись об ошибке. */
    private val failedMessages = messages + listOf(
        ChatMessage.Outgoing(
            id = "chat-1-user",
            text = "Make it rounder",
            sentAt = Instant.parse("2026-08-12T11:32:00Z"),
        ),
        ChatMessage.Failed(
            id = "chat-2-assistant",
            text = "Invalid SVG",
            sentAt = Instant.parse("2026-08-12T11:32:20Z"),
            jobId = FakeChatRepository.JOB_ID,
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
    fun `неотправленный промпт возвращается в поле и уходит из ленты`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        chatRepository.startError = IllegalStateException("network is down")
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
    fun `после неудачного старта лента не перезапрашивается`() = runTest(dispatcher) {
        // Задачи нет, промпт до сервера не дошёл — перечитывать нечего.
        chatRepository.chatResult = Result.success(chat)
        chatRepository.startError = IllegalStateException("boom")
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(1, chatRepository.getChatCallCount)
    }

    @Test
    fun `неудачный старт не перетирает новый черновик`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        chatRepository.startError = IllegalStateException("network is down")
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
    fun `скелетон ответа появляется сразу после старта задачи`() = runTest(dispatcher) {
        // Ждать теперь можно минутами — спиннера в кнопке отправки мало.
        chatRepository.chatResult = Result.success(chat)
        chatRepository.awaitResult = Result.success(GenerationOutcome.TimedOut)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        // TimedOut скелетон снимает, поэтому проверяем сам факт ожидания задачи.
        assertEquals(listOf(FakeChatRepository.JOB_ID), chatRepository.awaitedJobIds)
    }

    @Test
    fun `провал генерации перечитывает ленту и не возвращает текст в поле`() =
        runTest(dispatcher) {
            // Промпт уже лежит на сервере — вернуть его в поле значит поощрить
            // дубль, а после перезахода он всё равно всплывёт в ленте.
            chatRepository.chatResult = Result.success(chat)
            val viewModel = viewModel()
            advanceUntilIdle()

            chatRepository.awaitResult =
                Result.success(GenerationOutcome.Failed("Invalid SVG"))
            chatRepository.chatResult = Result.success(chat.copy(messages = failedMessages))
            viewModel.onDraftChange("Make it rounder")
            viewModel.send()
            advanceUntilIdle()

            val content = contentOf(viewModel)
            assertEquals(failedMessages, content.messages)
            assertEquals("", content.draft)
            assertEquals(false, content.isSending)
        }

    @Test
    fun `если ленту перечитать не удалось, пузырь ошибки рисуется локально`() =
        runTest(dispatcher) {
            chatRepository.chatResult = Result.success(chat)
            val viewModel = viewModel()
            advanceUntilIdle()

            chatRepository.awaitResult =
                Result.success(GenerationOutcome.Failed("Invalid SVG"))
            chatRepository.chatResult = Result.failure(IllegalStateException("network is down"))
            viewModel.onDraftChange("Make it rounder")
            viewModel.send()
            advanceUntilIdle()

            val content = contentOf(viewModel)
            // Оптимистичный промпт остался: на сервере он есть.
            assertEquals("Make it rounder", content.messages.filterIsInstance<ChatMessage.Outgoing>().last().text)
            assertEquals(
                "Invalid SVG",
                content.messages.filterIsInstance<ChatMessage.Failed>().single().text,
            )
            assertEquals(false, content.isSending)
        }

    @Test
    fun `сорвавшийся опрос показывает ошибку без служебного текста`() = runTest(dispatcher) {
        // Текст NetworkException служебный — пользователю его показывать нельзя,
        // подпись возьмёт UI из strings.xml.
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        chatRepository.awaitResult = Result.failure(NetworkException.Forbidden())
        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        val content = contentOf(viewModel)
        assertEquals(null, content.messages.filterIsInstance<ChatMessage.Failed>().single().text)
        // Лента читается, но перечитывать её нельзя: записи об исходе на сервере
        // нет, и она молча заменила бы пузырь ошибки пустотой.
        assertEquals(1, chatRepository.getChatCallCount)
        assertEquals(false, content.isSending)
    }

    @Test
    fun `лента перечитывается, пока ответ не появится в истории`() = runTest(dispatcher) {
        // Бэкенд отвечает на опрос статуса раньше, чем дописывает ответ в
        // историю: первая перечитанная лента приходит ещё без него.
        chatRepository.chatResults += Result.success(chat)
        chatRepository.chatResults += Result.success(chat.copy(messages = reloadedWithoutAnswer))
        chatRepository.chatResults += Result.success(chat.copy(messages = reloadedMessages))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        val content = contentOf(viewModel)
        assertEquals(reloadedMessages, content.messages)
        assertEquals(3, chatRepository.getChatCallCount)
        assertEquals(false, content.isSending)
    }

    @Test
    fun `ответ так и не появился в истории — лента остаётся своей`() = runTest(dispatcher) {
        // Запись в историю у бэкенда best-effort: она могла не пройти вовсе.
        // Заменить ленту на серверную значило бы стереть промпт и скелетон,
        // не дав взамен ничего.
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        chatRepository.chatResult = Result.success(chat.copy(messages = reloadedWithoutAnswer))
        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        val content = contentOf(viewModel)
        assertEquals(
            "Make it rounder",
            content.messages.filterIsInstance<ChatMessage.Outgoing>().last().text,
        )
        assertEquals(emptyList<ChatMessage.Pending>(), content.messages.filterIsInstance<ChatMessage.Pending>())
        assertEquals(false, content.isSending)
    }

    @Test
    fun `провал без записи в истории показывается локальным пузырём`() = runTest(dispatcher) {
        // Ошибку сервер назвал, но дописать её в историю не успел — текст берём
        // из ответа опроса, а не теряем вместе с перечитанной лентой.
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        chatRepository.awaitResult = Result.success(GenerationOutcome.Failed("Invalid SVG"))
        chatRepository.chatResult = Result.success(chat.copy(messages = reloadedWithoutAnswer))
        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        val content = contentOf(viewModel)
        assertEquals(
            "Invalid SVG",
            content.messages.filterIsInstance<ChatMessage.Failed>().single().text,
        )
        assertEquals(
            "Make it rounder",
            content.messages.filterIsInstance<ChatMessage.Outgoing>().last().text,
        )
        assertEquals(false, content.isSending)
    }

    @Test
    fun `дедлайн ожидания оставляет промпт и говорит, что генерация идёт`() =
        runTest(dispatcher) {
            // Это не провал: задача могла остаться считаться на сервере.
            chatRepository.chatResult = Result.success(chat)
            chatRepository.awaitResult = Result.success(GenerationOutcome.TimedOut)
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onDraftChange("Make it rounder")
            viewModel.send()
            advanceUntilIdle()

            val content = contentOf(viewModel)
            assertEquals("Make it rounder", content.messages.filterIsInstance<ChatMessage.Outgoing>().last().text)
            assertEquals(1, content.messages.filterIsInstance<ChatMessage.System.GenerationStillRunning>().size)
            assertEquals(emptyList<ChatMessage.Pending>(), content.messages.filterIsInstance<ChatMessage.Pending>())
            assertEquals("", content.draft)
            assertEquals(false, content.isSending)
            // Ленту не перечитывали: результата всё равно ещё нет.
            assertEquals(1, chatRepository.getChatCallCount)
        }

    @Test
    fun `Повторить отправляет промпт, стоящий над пузырём ошибки`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat.copy(messages = failedMessages))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.retryGeneration("chat-2-assistant")
        advanceUntilIdle()

        assertEquals(listOf("Make it rounder"), chatRepository.sentPrompts)
    }

    @Test
    fun `Повторить во время генерации игнорируется`() = runTest(dispatcher) {
        // Иначе одно нажатие «на всякий случай» завело бы вторую задачу.
        chatRepository.chatResult = Result.success(chat.copy(messages = failedMessages))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.retryGeneration("chat-2-assistant")
        viewModel.retryGeneration("chat-2-assistant")
        advanceUntilIdle()

        assertEquals(listOf("Make it rounder"), chatRepository.sentPrompts)
    }

    @Test
    fun `Повторить по неизвестному сообщению ничего не отправляет`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.retryGeneration("no-such-message")
        advanceUntilIdle()

        assertEquals(emptyList<String>(), chatRepository.sentPrompts)
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
