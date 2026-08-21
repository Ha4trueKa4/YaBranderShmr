package com.yandex.avatargenerator.feature.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
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
 */
internal class ProjectViewModel(
    private val chatRepository: ChatRepository,
    private val projectId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProjectUiState>(ProjectUiState.Loading)
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

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
     * Промпт кладётся в ленту сразу, до ответа сервера: генерация идёт десятками
     * секунд, и всё это время экран не должен выглядеть так, будто нажатие
     * пропало. Поле очищается там же — иначе ввод «залипает» с уже отправленным
     * текстом.
     *
     * По успеху лента перечитывается целиком: ни номера новой версии, ни её
     * `version_id` в ответе генерации нет, а сам сервер уже дописал в историю
     * пару «промпт → версия» — без перезапроса оптимистичное сообщение осталось
     * бы в ленте со своим локальным ключом и продублировало серверную запись при
     * следующем открытии экрана.
     *
     * По ошибке оптимистичное сообщение убирается, а текст возвращается в поле,
     * чтобы не пропасть без следа. Перезапрашивать ленту тут нечего: при 422 и
     * 503 бэкенд промпт в историю не пишет.
     */
    fun send() {
        val state = _uiState.value
        if (state !is ProjectUiState.Content || state.isSending) return
        val text = state.draft.trim()
        if (text.isEmpty()) return

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
                onFailure = {
                    _uiState.updateContent { content ->
                        content.copy(
                            messages = content.messages.filterNot { it.id == pending.id },
                            // Новый черновик не перетираем: пользователь мог начать
                            // писать следующее сообщение, пока шла генерация.
                            draft = if (content.draft.isEmpty()) text else content.draft,
                            isSending = false,
                        )
                    }
                },
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
        _uiState.updateContent { content ->
            if (chat == null) {
                content.copy(isSending = false)
            } else {
                content.copy(
                    title = chat.title,
                    isPublic = chat.isPublic,
                    messages = chat.messages,
                    isSending = false,
                )
            }
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
                    ProjectUiState.Content(
                        title = chat.title,
                        isPublic = chat.isPublic,
                        messages = chat.messages,
                    )
                },
                onFailure = { ProjectUiState.Error },
            )
        }
    }
}

/** Правка состояния, только если экран всё ещё показывает содержимое. */
private inline fun MutableStateFlow<ProjectUiState>.updateContent(
    crossinline block: (ProjectUiState.Content) -> ProjectUiState.Content,
) = update { state -> if (state is ProjectUiState.Content) block(state) else state }
