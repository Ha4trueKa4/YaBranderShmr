package com.yandex.avatargenerator.feature.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.ApiConfig
import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.VersionsRepository
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.domain.model.ProjectChat
import kotlinx.coroutines.delay
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
     * Промпт кладётся в ленту сразу, до ответа сервера: генерация идёт минутами,
     * и всё это время экран не должен выглядеть так, будто нажатие пропало. Поле
     * очищается там же — иначе ввод «залипает» с уже отправленным текстом.
     *
     * По успеху лента перечитывается целиком: ни номера новой версии, ни её
     * `version_id` в ответе генерации нет, а сам сервер уже дописал в историю
     * пару «промпт → версия» — без перезапроса оптимистичное сообщение осталось
     * бы в ленте со своим локальным ключом и продублировало серверную запись при
     * следующем открытии экрана.
     *
     * Таймаут — не отказ: сервер доводит генерацию до конца независимо от того,
     * дождался клиент или нет, поэтому вместо ошибки ждём версию опросом (см.
     * [awaitGeneratedVersion]). На остальных ошибках (400, 403, 422, 503) версии
     * не появится — оптимистичное сообщение убирается, а текст возвращается в
     * поле, чтобы не пропасть без следа.
     */
    fun send() {
        val state = _uiState.value
        if (state !is ProjectUiState.Content || state.isSending) return
        val text = state.draft.trim()
        if (text.isEmpty()) return

        // Снимаем до отправки: по смене этой версии узнаём, что генерация дошла.
        val baselineVersionId = state.messages.lastVersionId()
        val pending = ChatMessage.Outgoing(
            id = UUID.randomUUID().toString(),
            text = text,
            sentAt = Instant.now(),
        )
        _uiState.updateContent {
            it.copy(draft = "", messages = it.messages + pending, isSending = true)
        }

        viewModelScope.launch {
            // TODO: индикация неотправленного сообщения со статусом и повтором.
            chatRepository.sendPrompt(projectId, text).fold(
                onSuccess = { reloadAfterSend() },
                onFailure = { error ->
                    if (error is NetworkException.Timeout) {
                        awaitGeneratedVersion(pending, text, baselineVersionId)
                    } else {
                        dropPending(pending, text)
                    }
                },
            )
        }
    }

    /**
     * Ждёт версию, которую сервер дописывает уже после клиентского таймаута.
     *
     * Опрашиваем ленту, а не `/versions` напрямую: [ChatRepository.getChat] сам
     * ходит и за версиями, и за историей, так что при удачном опросе экран сразу
     * получает готовое содержимое, а не повод сделать ещё один запрос.
     *
     * Упавший опрос цикл не прерывает: сеть может лежать ровно на этом тике, а
     * версия на сервере от этого никуда не денется. Если за всё окно новой
     * версии не появилось — считаем генерацию неудавшейся и ведём себя как при
     * обычной ошибке.
     */
    private suspend fun awaitGeneratedVersion(
        pending: ChatMessage.Outgoing,
        text: String,
        baselineVersionId: String?,
    ) {
        val attempts = ApiConfig.GenerationPollWindowMillis / ApiConfig.GenerationPollIntervalMillis
        repeat(attempts.toInt()) {
            delay(ApiConfig.GenerationPollIntervalMillis)
            val chat = chatRepository.getChat(projectId).getOrNull() ?: return@repeat
            if (chat.messages.lastVersionId() != baselineVersionId) {
                applyChat(chat)
                return
            }
        }
        dropPending(pending, text)
    }

    /**
     * Убирает оптимистичное сообщение и возвращает текст в поле ввода.
     *
     * Новый черновик не перетираем: пользователь мог начать писать следующее
     * сообщение, пока шла генерация.
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
     * есть (с оптимистичным сообщением) вместо [ProjectUiState.Error]: показать
     * пользователю ошибку там, где всё сработало, было бы враньём.
     */
    private suspend fun reloadAfterSend() {
        val chat = chatRepository.getChat(projectId).getOrNull()
        if (chat == null) {
            _uiState.updateContent { it.copy(isSending = false) }
        } else {
            applyChat(chat)
        }
    }

    /** Серверная лента заменяет локальную целиком — оптимистичное сообщение уходит с ней. */
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

/** Версия последнего ответа ИИ в ленте; `null` — ответов ещё не было. */
private fun List<ChatMessage>.lastVersionId(): String? =
    filterIsInstance<ChatMessage.Incoming>().lastOrNull()?.versionId

/** Правка состояния, только если экран всё ещё показывает содержимое. */
private inline fun MutableStateFlow<ProjectUiState>.updateContent(
    crossinline block: (ProjectUiState.Content) -> ProjectUiState.Content,
) = update { state -> if (state is ProjectUiState.Content) block(state) else state }
