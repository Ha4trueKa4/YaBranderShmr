package com.yandex.avatargenerator.feature.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.VersionsRepository
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.GenerationOutcome
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * ViewModel экрана проекта: грузит по [projectId] диалог с ИИ через
 * [ChatRepository], держит черновик ввода и отправляет промпты на генерацию.
 *
 * Название и видимость проекта приходят в том же ответе репозитория — своего
 * запроса за проектом ViewModel не делает: репозиторий всё равно ходит за ним,
 * чтобы взять ссылку на последнюю версию, и второй такой же запрос был бы лишним.
 *
 * Превью прошлых версий догружаются отдельно через [VersionsRepository]: история
 * чата отдаёт только `version_id`, а ссылка на картинку у каждой версии своя и
 * запрашивается по одной (`GET /projects/{id}/versions/{versionId}`) — и только
 * для тех пузырей, которые дошли до экрана (см. [onPreviewNeeded]).
 *
 * **Ожидание генерации живёт в `viewModelScope`.** Ушёл с экрана — опрос умер;
 * бэкенд доводит генерацию до конца сам и кладёт результат в историю, так что
 * при возврате экран перечитает ленту и увидит его. Ни `WorkManager`, ни
 * хранение `job_id` на диске ради этого не заводим.
 */
internal class ProjectViewModel(
    private val chatRepository: ChatRepository,
    private val versionsRepository: VersionsRepository,
    private val projectId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectUiState>(ProjectUiState.Loading)
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    /**
     * Уже известные ссылки на превью, versionId → url. Переживают перечитывание
     * ленты: сервер отдаёт ссылку только для последней версии, и без кэша все
     * остальные пузыри теряли бы картинку на каждом обновлении.
     *
     * Presigned-ссылка живёт час — этого хватает на сеанс работы с экраном.
     */
    private val previewUrls = mutableMapOf<String, String>()

    /**
     * Версии, за ссылкой на которые уже ходили — успешно или нет. Пузырь в
     * `LazyColumn` пересоздаётся на каждом заходе в зону видимости, и без этого
     * набора прокрутка ленты вверх-вниз слала бы запрос за запросом.
     */
    private val requestedVersions = mutableSetOf<String>()

    init {
        load()
    }

    /** Повтор после ошибки. */
    fun retry() = load()

    /** Ввод обрезаем по [MaxPromptBytes] — см. комментарий у константы. */
    fun onDraftChange(text: String) {
        _uiState.updateContent { it.copy(draft = limitPromptBytes(text)) }
    }

    /**
     * Отправляет черновик на генерацию.
     *
     * Поле очищается сразу, до ответа сервера: иначе ввод «залипает» с уже
     * отправленным текстом. Вернуть текст обратно можно ровно в одном случае —
     * если промпт до сервера не дошёл (см. [dropPending]).
     */
    fun send() {
        val state = _uiState.value
        if (state !is ProjectUiState.Content || state.isSending) return
        val text = state.draft.trim()
        if (text.isEmpty()) return

        _uiState.updateContent { it.copy(draft = "") }
        generate(text)
    }

    /**
     * Повторяет промпт, стоящий над пузырём ошибки [messageId].
     *
     * Это **новая задача и новая пара сообщений в истории** — бэкенд ничего не
     * перезаписывает, так что дубль промпта на сервере неизбежен. Сам пузырь
     * ошибки не убираем: он лежит в истории на сервере и всё равно вернётся при
     * следующем перечитывании ленты.
     */
    fun retryGeneration(messageId: String) {
        val state = _uiState.value
        if (state !is ProjectUiState.Content || state.isSending) return
        val text = state.messages.promptAbove(messageId) ?: return

        generate(text)
    }

    /**
     * Общий путь отправки: оптимистичный пузырь промпта → старт задачи →
     * скелетон ответа → ожидание результата.
     *
     * Промпт кладётся в ленту сразу: генерация идёт минутами, и всё это время
     * экран не должен выглядеть так, будто нажатие пропало.
     */
    private fun generate(text: String) {
        val prompt = ChatMessage.Outgoing(
            id = UUID.randomUUID().toString(),
            text = text,
            sentAt = Instant.now(),
        )
        _uiState.updateContent {
            it.copy(messages = it.messages + prompt, isSending = true)
        }

        viewModelScope.launch {
            chatRepository.startGeneration(projectId, text).fold(
                onSuccess = { jobId -> awaitGeneration(jobId) },
                // Единственный случай, когда промпт можно убрать из ленты и
                // вернуть в поле: до сервера он не дошёл, в историю не попал.
                onFailure = { dropPending(prompt, text) },
            )
        }
    }

    /**
     * Ждёт результат задачи [jobId], показывая на её месте скелетон ответа.
     *
     * По [GenerationOutcome.Failed] лента перечитывается: сервер уже положил в
     * историю и промпт, и запись об ошибке — после перезахода экран должен
     * выглядеть так же. Не удалось перечитать — рисуем пузырь ошибки локально,
     * оптимистичный промпт при этом не трогаем: на сервере он есть.
     */
    private suspend fun awaitGeneration(jobId: String) {
        val skeleton = ChatMessage.Pending(id = UUID.randomUUID().toString(), jobId = jobId)
        _uiState.updateContent { it.copy(messages = it.messages + skeleton) }

        val outcome = chatRepository.awaitGeneration(projectId, jobId)
            // Опрос сорвался — о судьбе генерации ничего не известно. Текст
            // NetworkException пользователю не показываем: он служебный.
            .getOrElse { GenerationOutcome.Failed(message = null) }

        when (outcome) {
            is GenerationOutcome.Done -> reloadAfterSend()
            is GenerationOutcome.Failed -> showFailure(skeleton, outcome.message)
            GenerationOutcome.TimedOut -> showStillRunning(skeleton)
        }
    }

    /** Заменяет скелетон пузырём ошибки, если ленту перечитать не удалось. */
    private suspend fun showFailure(skeleton: ChatMessage.Pending, message: String?) {
        val chat = chatRepository.getChat(projectId).getOrNull()
        if (chat != null) {
            applyChat(chat)
            return
        }
        val failure = ChatMessage.Failed(
            id = skeleton.id,
            text = message,
            sentAt = Instant.now(),
        )
        replaceSkeleton(skeleton, failure)
    }

    /**
     * Дедлайн вышел — это не провал: задача могла остаться считаться. Убираем
     * скелетон и говорим об этом пилюлей, промпт в ленте оставляем.
     */
    private fun showStillRunning(skeleton: ChatMessage.Pending) {
        replaceSkeleton(
            skeleton,
            ChatMessage.System.GenerationStillRunning(id = skeleton.id),
        )
    }

    /** Скелетон меняется на месте — чтобы сообщение встало туда же, где он стоял. */
    private fun replaceSkeleton(skeleton: ChatMessage.Pending, replacement: ChatMessage) {
        _uiState.updateContent { content ->
            content.copy(
                messages = content.messages.map { if (it.id == skeleton.id) replacement else it },
                isSending = false,
            )
        }
    }

    /**
     * Убирает оптимистичное сообщение и возвращает текст в поле ввода.
     *
     * Новый черновик не перетираем: пользователь мог начать писать следующее
     * сообщение, пока шла отправка.
     */
    private fun dropPending(pending: ChatMessage.Outgoing, text: String) {
        _uiState.updateContent { content ->
            content.copy(
                messages = content.messages.filterNot { it.id == pending.id },
                draft = if (content.draft.isEmpty()) text else content.draft,
                isSending = false,
            )
        }
    }

    /**
     * Перечитывает ленту после удачной генерации, не показывая скелетон: экран
     * уже с содержимым, и мигать им из-за фонового обновления незачем.
     *
     * Если перезапрос не удался, версия всё равно создана — оставляем ленту как
     * есть вместо [ProjectUiState.Error]: показать пользователю ошибку там, где
     * всё сработало, было бы враньём. Скелетон при этом убираем — ждать больше
     * нечего.
     */
    private suspend fun reloadAfterSend() {
        val chat = chatRepository.getChat(projectId).getOrNull()
        if (chat == null) {
            _uiState.updateContent { content ->
                content.copy(
                    messages = content.messages.filterNot { it is ChatMessage.Pending },
                    isSending = false,
                )
            }
        } else {
            applyChat(chat)
        }
    }

    /**
     * Серверная лента заменяет локальную целиком — оптимистичный промпт и
     * скелетон уходят вместе с ней.
     */
    private fun applyChat(chat: ProjectChat) {
        rememberPreviews(chat.messages)
        _uiState.updateContent { content ->
            content.copy(
                title = chat.title,
                isPublic = chat.isPublic,
                messages = chat.messages.withKnownPreviews(),
                isSending = false,
            )
        }
    }

    /**
     * Пузырь версии [versionId] появился на экране — при первом показе идём за
     * ссылкой на превью.
     *
     * История чата отдаёт только `version_id`, а ссылку на картинку сервер
     * прикладывает лишь к последней версии — у остальных пузырей она пустая, и
     * без этой догрузки в ленте была бы видна одна последняя картинка.
     *
     * Ходим по требованию, а не пачкой при открытии экрана: у долгого проекта
     * это были бы десятки запросов разом ради картинок, которых пользователь
     * может и не пролистать. Повторно по той же версии не ходим — ни при
     * возврате пузыря в зону видимости, ни после ошибки: пузырь просто
     * останется без картинки, рушить из-за этого экран незачем.
     */
    fun onPreviewNeeded(versionId: String) {
        if (versionId in previewUrls || !requestedVersions.add(versionId)) return
        viewModelScope.launch {
            versionsRepository.getVersionUrl(projectId, versionId)
                .onSuccess { url -> applyPreview(versionId, url) }
        }
    }

    /** Дорисовывает пришедшую ссылку всем пузырям этой версии. */
    private fun applyPreview(versionId: String, url: String) {
        previewUrls[versionId] = url
        _uiState.updateContent { content ->
            content.copy(
                messages = content.messages.map { message ->
                    if (message is ChatMessage.Incoming &&
                        message.versionId == versionId &&
                        message.previewUrl == null
                    ) {
                        message.copy(previewUrl = url)
                    } else {
                        message
                    }
                },
            )
        }
    }

    /** Ссылку последней версии сервер присылает сам — кладём её в кэш как свою. */
    private fun rememberPreviews(messages: List<ChatMessage>) {
        messages.filterIsInstance<ChatMessage.Incoming>().forEach { message ->
            message.previewUrl?.let { previewUrls[message.versionId] = it }
        }
    }

    private fun List<ChatMessage>.withKnownPreviews(): List<ChatMessage> = map { message ->
        if (message is ChatMessage.Incoming && message.previewUrl == null) {
            previewUrls[message.versionId]?.let { message.copy(previewUrl = it) } ?: message
        } else {
            message
        }
    }

    /**
     * Пустая лента — валидное состояние (новый проект, недоступная история у
     * не-автора), а не [ProjectUiState.Error]: ошибку репозиторий отдаёт только
     * когда не загрузился сам проект.
     */
    private fun load() {
        _uiState.value = ProjectUiState.Loading
        viewModelScope.launch {
            _uiState.value = chatRepository.getChat(projectId).fold(
                onSuccess = { chat ->
                    rememberPreviews(chat.messages)
                    ProjectUiState.Content(
                        title = chat.title,
                        isPublic = chat.isPublic,
                        messages = chat.messages.withKnownPreviews(),
                    )
                },
                onFailure = { ProjectUiState.Error },
            )
        }
    }
}

/**
 * Текст ближайшего промпта над сообщением [messageId] — тот, который и надо
 * повторить. `null` — сообщения в ленте уже нет или промпта над ним не нашлось.
 */
private fun List<ChatMessage>.promptAbove(messageId: String): String? {
    val index = indexOfFirst { it.id == messageId }
    if (index < 0) return null
    return take(index).filterIsInstance<ChatMessage.Outgoing>().lastOrNull()?.text
}

/** Правка состояния, только если экран всё ещё показывает содержимое. */
private inline fun MutableStateFlow<ProjectUiState>.updateContent(
    crossinline block: (ProjectUiState.Content) -> ProjectUiState.Content,
) = update { state -> if (state is ProjectUiState.Content) block(state) else state }
