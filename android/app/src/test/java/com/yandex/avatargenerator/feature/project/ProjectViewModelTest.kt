package com.yandex.avatargenerator.feature.project

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.GenerationModel
import com.yandex.avatargenerator.feature.project.domain.model.GenerationOutcome
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat
import com.yandex.avatargenerator.feature.project.fake.FakeGenerationModelRepository
import com.yandex.avatargenerator.feature.project.ui.MaxPromptBytes
import com.yandex.avatargenerator.feature.project.ui.ProjectUiState
import com.yandex.avatargenerator.feature.project.ui.ProjectViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
internal class ProjectViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var chatRepository: FakeChatRepository
    private lateinit var versionsRepository: FakeVersionsRepository
    private lateinit var projectRepository: FakeProjectRepository
    private lateinit var modelRepository: FakeGenerationModelRepository

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

    /** Лента во время генерации: промпт и серверная заглушка активной задачи. */
    private val stillGenerating = reloadedWithoutAnswer + ChatMessage.Pending(
        id = "chat-2-assistant",
        jobId = FakeChatRepository.JOB_ID,
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

    /** Лента, какой её отдаёт сервер после отмены: промпт и запись об отмене. */
    private val cancelledMessages = messages + listOf(
        ChatMessage.Outgoing(
            id = "chat-1-user",
            text = "Make it rounder",
            sentAt = Instant.parse("2026-08-12T11:32:00Z"),
        ),
        ChatMessage.Cancelled(
            id = "chat-2-assistant",
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
        projectRepository = FakeProjectRepository()
        modelRepository = FakeGenerationModelRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** [initialJobId] с дефолтом — остальным тестам ждать нечего. */
    private fun viewModel(initialJobId: String? = null) = ProjectViewModel(
        chatRepository,
        versionsRepository,
        projectRepository,
        modelRepository,
        PROJECT_ID,
        initialJobId,
    )

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
    fun `экран, открытый с задачей первичной генерации, ждёт её и перечитывает ленту`() =
        runTest(dispatcher) {
            // Так на экран уходит форма создания: промпт бэкенд записал в историю
            // сам, экрану остаётся показать скелетон ответа и дождаться его.
            chatRepository.chatResults += Result.success(chat)
            chatRepository.chatResults += Result.success(chat.copy(messages = reloadedWithoutAnswer))
            chatRepository.chatResults += Result.success(chat.copy(messages = reloadedMessages))
            val viewModel = viewModel(initialJobId = FakeChatRepository.JOB_ID)

            // Перечитывание ленты упирается в паузу между попытками — на ней и
            // виден скелетон, поставленный на время ожидания.
            runCurrent()
            val waiting = contentOf(viewModel)
            assertEquals(1, waiting.messages.filterIsInstance<ChatMessage.Pending>().size)
            assertTrue(waiting.isSending)

            advanceUntilIdle()

            val content = contentOf(viewModel)
            assertEquals(listOf(FakeChatRepository.JOB_ID), chatRepository.awaitedJobIds)
            assertEquals(reloadedMessages, content.messages)
            assertFalse(content.isSending)
        }

    @Test
    fun `без задачи первичной генерации экран ничего не ждёт`() = runTest(dispatcher) {
        // Обычный заход из списка чатов: лента уже полная, ждать нечего.
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(emptyList<String>(), chatRepository.awaitedJobIds)
        assertFalse(contentOf(viewModel).isSending)
    }

    @Test
    fun `сорвавшаяся первая загрузка не съедает задачу первичной генерации`() =
        runTest(dispatcher) {
            // Ждать на Error некуда: скелетон и ответ рисует только Content.
            // Значит задача должна дожить до успешного retry, а не пропасть.
            chatRepository.chatResults += Result.failure(IllegalStateException("network is down"))
            val viewModel = viewModel(initialJobId = FakeChatRepository.JOB_ID)
            advanceUntilIdle()

            assertEquals(ProjectUiState.Error, viewModel.uiState.value)
            assertEquals(emptyList<String>(), chatRepository.awaitedJobIds)

            chatRepository.chatResult = Result.success(chat)
            viewModel.retry()
            advanceUntilIdle()

            assertEquals(listOf(FakeChatRepository.JOB_ID), chatRepository.awaitedJobIds)
        }

    @Test
    fun `перезаход в чат во время генерации поднимает ожидание заново`() = runTest(dispatcher) {
        // Опрос живёт в viewModelScope и уходом с экрана обрывается. При
        // возврате незакрытую задачу видно по скелетону в самой ленте: пока
        // задача считается, бэкенд отдаёт её заглушкой в истории.
        chatRepository.chatResults += Result.success(chat.copy(messages = stillGenerating))
        chatRepository.chatResults += Result.success(chat.copy(messages = reloadedWithoutAnswer))
        chatRepository.chatResults += Result.success(chat.copy(messages = reloadedMessages))
        val viewModel = viewModel()

        // Перечитывание ленты упирается в паузу между попытками — на ней и
        // виден скелетон, поставленный на время ожидания.
        runCurrent()
        val waiting = contentOf(viewModel)
        // Ровно один: серверный скелетон уже в ленте, своего сверху не ставим.
        assertEquals(1, waiting.messages.filterIsInstance<ChatMessage.Pending>().size)
        // Отправка заблокирована: второй POST /generate бэкенд всё равно отвергнет.
        assertTrue(waiting.isSending)

        advanceUntilIdle()

        val content = contentOf(viewModel)
        assertEquals(listOf(FakeChatRepository.JOB_ID), chatRepository.awaitedJobIds)
        assertEquals(reloadedMessages, content.messages)
        assertFalse(content.isSending)
    }

    @Test
    fun `после перезахода генерацию можно остановить`() = runTest(dispatcher) {
        // Задача для кнопки остановки берётся из скелетона в ленте, поэтому
        // работает и тогда, когда её job_id экран сам никогда не видел.
        chatRepository.awaitGate = CompletableDeferred()
        chatRepository.chatResult = Result.success(chat.copy(messages = stillGenerating))
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(contentOf(viewModel).canCancel)

        viewModel.cancelGeneration()
        advanceUntilIdle()

        assertEquals(listOf(FakeChatRepository.JOB_ID), chatRepository.cancelledJobIds)
    }

    @Test
    fun `задача первичной генерации не дублирует скелетон из ленты`() = runTest(dispatcher) {
        // Экран открыт формой создания, и та же задача уже пришла заглушкой в
        // истории — ждём её один раз и одним скелетоном.
        chatRepository.chatResult = Result.success(chat.copy(messages = stillGenerating))
        val viewModel = viewModel(initialJobId = FakeChatRepository.JOB_ID)
        runCurrent()

        assertEquals(
            1,
            contentOf(viewModel).messages.filterIsInstance<ChatMessage.Pending>().size,
        )

        advanceUntilIdle()
        assertEquals(listOf(FakeChatRepository.JOB_ID), chatRepository.awaitedJobIds)
    }

    @Test
    fun `retry не запускает ожидание первичной генерации второй раз`() = runTest(dispatcher) {
        // retry() зовёт тот же load(), а задача уже отжидана.
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel(initialJobId = FakeChatRepository.JOB_ID)
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(listOf(FakeChatRepository.JOB_ID), chatRepository.awaitedJobIds)
    }

    @Test
    fun `остановка шлёт отмену по идущей задаче, не трогая скелетон`() = runTest(dispatcher) {
        // Скелетон снимет исход, который принесёт всё тот же опрос статуса:
        // задача останавливается не мгновенно, и убрать его раньше — соврать.
        chatRepository.chatResult = Result.success(chat)
        val gate = CompletableDeferred<Unit>()
        chatRepository.awaitGate = gate
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()
        viewModel.cancelGeneration()
        advanceUntilIdle()

        assertEquals(listOf(FakeChatRepository.JOB_ID), chatRepository.cancelledJobIds)
        val content = contentOf(viewModel)
        assertTrue(content.isCancelling)
        assertFalse("отмену не повторяем", content.canCancel)
        assertEquals(1, content.messages.filterIsInstance<ChatMessage.Pending>().size)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `вторая остановка подряд второго запроса не шлёт`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        val gate = CompletableDeferred<Unit>()
        chatRepository.awaitGate = gate
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()
        viewModel.cancelGeneration()
        viewModel.cancelGeneration()
        advanceUntilIdle()

        assertEquals(listOf(FakeChatRepository.JOB_ID), chatRepository.cancelledJobIds)

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `остановка без идущей генерации ничего не шлёт`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.cancelGeneration()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), chatRepository.cancelledJobIds)
    }

    @Test
    fun `неудачная отмена снимает только свой флаг — ожидание продолжается`() =
        runTest(dispatcher) {
            chatRepository.chatResult = Result.success(chat)
            chatRepository.cancelError = NetworkException.NotFound()
            val gate = CompletableDeferred<Unit>()
            chatRepository.awaitGate = gate
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.onDraftChange("Make it rounder")
            viewModel.send()
            advanceUntilIdle()
            viewModel.cancelGeneration()
            advanceUntilIdle()

            val content = contentOf(viewModel)
            assertFalse(content.isCancelling)
            assertTrue(content.isSending)
            // Нажать «остановить» можно ещё раз.
            assertTrue(content.canCancel)

            gate.complete(Unit)
            advanceUntilIdle()
        }

    @Test
    fun `отменённая генерация показывается перечитанной лентой`() = runTest(dispatcher) {
        // Запись об отмене бэкенд пишет в историю — после перезахода экран
        // выглядит так же.
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        chatRepository.awaitResult = Result.success(GenerationOutcome.Cancelled)
        chatRepository.chatResult = Result.success(chat.copy(messages = cancelledMessages))
        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        val content = contentOf(viewModel)
        assertEquals(cancelledMessages, content.messages)
        assertEquals(false, content.isSending)
        assertEquals(false, content.isCancelling)
    }

    @Test
    fun `отмена без записи в истории рисуется локальной пилюлей`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        chatRepository.awaitResult = Result.success(GenerationOutcome.Cancelled)
        chatRepository.chatResult = Result.failure(IllegalStateException("network is down"))
        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        val content = contentOf(viewModel)
        // Промпт остался: на сервере он есть, версии отмена не создала.
        assertEquals(
            "Make it rounder",
            content.messages.filterIsInstance<ChatMessage.Outgoing>().last().text,
        )
        assertEquals(1, content.messages.filterIsInstance<ChatMessage.Cancelled>().size)
        assertEquals(emptyList<ChatMessage.Pending>(), content.messages.filterIsInstance<ChatMessage.Pending>())
        assertEquals(false, content.isSending)
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

    @Test
    fun `смена видимости переключает чип сразу и шлёт PATCH`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat) // chat.isPublic = true
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.setVisibility(false)
        // До advanceUntilIdle: обновление оптимистичное, чип уже приватный.
        assertEquals(false, contentOf(viewModel).isPublic)

        advanceUntilIdle()
        assertEquals(false, contentOf(viewModel).isPublic)
        assertEquals(1, projectRepository.updateProjectCallCount)
        assertEquals(false, projectRepository.lastUpdatedIsPublic)
    }

    @Test
    fun `неудачный PATCH откатывает видимость`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        projectRepository.updateResult = Result.failure(NetworkException.Forbidden())
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.setVisibility(false)
        advanceUntilIdle()

        // Сервер отказал — чип вернулся к прежнему значению.
        assertEquals(true, contentOf(viewModel).isPublic)
    }

    @Test
    fun `выбор текущей видимости PATCH не шлёт`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat) // уже публичный
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.setVisibility(true)
        advanceUntilIdle()

        assertEquals(0, projectRepository.updateProjectCallCount)
    }


    @Test
    fun `справочник моделей тянется лениво — только при первом открытии шторки`() =
        runTest(dispatcher) {
            chatRepository.chatResult = Result.success(chat)
            val viewModel = viewModel()
            advanceUntilIdle()

            // `/models` — статический справочник, который можно и не открыть.
            assertEquals(0, modelRepository.modelsCallCount)

            viewModel.openModelPicker()
            advanceUntilIdle()

            assertEquals(1, modelRepository.modelsCallCount)
            assertEquals(FakeGenerationModelRepository.MODELS, pickerOf(viewModel).models)
            assertTrue(pickerOf(viewModel).isSheetVisible)
        }

    @Test
    fun `второе открытие шторки за справочником не ходит`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.openModelPicker()
        advanceUntilIdle()
        viewModel.dismissModelPicker()
        viewModel.openModelPicker()
        advanceUntilIdle()

        assertEquals(1, modelRepository.modelsCallCount)
    }

    @Test
    fun `сохранённая модель читается при старте, не дожидаясь справочника`() =
        runTest(dispatcher) {
            chatRepository.chatResult = Result.success(chat)
            modelRepository.selectedModelId = FakeGenerationModelRepository.LUNA_ID
            val viewModel = viewModel()

            advanceUntilIdle()

            // Иначе чип моргнул бы дефолтом до первого открытия шторки.
            assertEquals(FakeGenerationModelRepository.LUNA_ID, pickerOf(viewModel).selectedModelId)
            assertEquals(0, modelRepository.modelsCallCount)
        }

    @Test
    fun `выбор модели закрывает шторку и уходит в настройки`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.openModelPicker()
        advanceUntilIdle()

        viewModel.selectModel(FakeGenerationModelRepository.LUNA_ID)
        advanceUntilIdle()

        assertEquals(FakeGenerationModelRepository.LUNA_ID, pickerOf(viewModel).selectedModelId)
        assertFalse(pickerOf(viewModel).isSheetVisible)
        assertEquals(listOf(FakeGenerationModelRepository.LUNA_ID), modelRepository.savedModelIds)
    }

    @Test
    fun `промпт уходит с выбранной моделью`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        chatRepository.chatResults += Result.success(chat.copy(messages = reloadedMessages))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.openModelPicker()
        advanceUntilIdle()
        viewModel.selectModel(FakeGenerationModelRepository.LUNA_ID)

        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(listOf(FakeGenerationModelRepository.LUNA_ID), chatRepository.sentModels)
    }

    @Test
    fun `сохранённой модели нет в справочнике — откат на дефолт с перезаписью`() =
        runTest(dispatcher) {
            // Бэкенд поменял allow-list либо выбор приехал с другой версии приложения.
            chatRepository.chatResult = Result.success(chat)
            modelRepository.selectedModelId = "retired-model/latest"
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.openModelPicker()
            advanceUntilIdle()

            assertEquals(GenerationModel.DefaultId, pickerOf(viewModel).selectedModelId)
            assertEquals(listOf(GenerationModel.DefaultId), modelRepository.savedModelIds)
        }

    @Test
    fun `нет в справочнике и дефолта — берём первую доступную`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        modelRepository.selectedModelId = "retired-model/latest"
        modelRepository.modelsResult = Result.success(
            listOf(GenerationModel(id = FakeGenerationModelRepository.LUNA_ID, owner = "OpenAI")),
        )
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.openModelPicker()
        advanceUntilIdle()

        assertEquals(FakeGenerationModelRepository.LUNA_ID, pickerOf(viewModel).selectedModelId)
        assertEquals(listOf(FakeGenerationModelRepository.LUNA_ID), modelRepository.savedModelIds)
    }

    @Test
    fun `пустой справочник — та же ошибка шторки, что и провал запроса`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        modelRepository.modelsResult = Result.success(emptyList())
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.openModelPicker()
        advanceUntilIdle()

        assertTrue(pickerOf(viewModel).hasError)
        assertEquals(emptyList<GenerationModel>(), pickerOf(viewModel).models)
    }

    @Test
    fun `провал справочника не блокирует отправку промпта`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        chatRepository.chatResults += Result.success(chat.copy(messages = reloadedMessages))
        modelRepository.modelsResult = Result.failure(NetworkException.NoConnection(null))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.openModelPicker()
        advanceUntilIdle()
        viewModel.dismissModelPicker()

        viewModel.onDraftChange("Make it rounder")
        viewModel.send()
        advanceUntilIdle()

        assertTrue(pickerOf(viewModel).hasError)
        assertEquals(listOf("Make it rounder"), chatRepository.sentPrompts)
        // В теле уедет сохранённый id — разбираться с ним будет сервер.
        assertEquals(listOf(GenerationModel.DefaultId), chatRepository.sentModels)
    }

    @Test
    fun `ретрай после ошибки справочника перечитывает список`() = runTest(dispatcher) {
        chatRepository.chatResult = Result.success(chat)
        modelRepository.modelsResult = Result.failure(NetworkException.NoConnection(null))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.openModelPicker()
        advanceUntilIdle()

        modelRepository.modelsResult = Result.success(FakeGenerationModelRepository.MODELS)
        viewModel.retryModels()
        advanceUntilIdle()

        assertFalse(pickerOf(viewModel).hasError)
        assertEquals(FakeGenerationModelRepository.MODELS, pickerOf(viewModel).models)
    }

    @Test
    fun `retryGeneration берёт текущую модель, а не ту, на которой промпт упал`() =
        runTest(dispatcher) {
            // По контракту это новая задача и новая пара сообщений — повторить
            // на другой модели осмысленно.
            chatRepository.chatResult = Result.success(chat.copy(messages = failedMessages))
            chatRepository.awaitResult = Result.success(GenerationOutcome.Done(previewUrl = null))
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.openModelPicker()
            advanceUntilIdle()
            viewModel.selectModel(FakeGenerationModelRepository.LUNA_ID)

            viewModel.retryGeneration("chat-2-assistant")
            advanceUntilIdle()

            assertEquals(listOf("Make it rounder"), chatRepository.sentPrompts)
            assertEquals(listOf(FakeGenerationModelRepository.LUNA_ID), chatRepository.sentModels)
        }

    private fun pickerOf(viewModel: ProjectViewModel) = contentOf(viewModel).modelPicker

    private fun contentOf(viewModel: ProjectViewModel): ProjectUiState.Content =
        viewModel.uiState.value as ProjectUiState.Content

    private companion object {
        const val PROJECT_ID = "project-1"
        const val LATEST_URL = "https://s3.example/v2.svg"
        const val OLD_URL = "https://s3.example/v1.svg"
    }
}
