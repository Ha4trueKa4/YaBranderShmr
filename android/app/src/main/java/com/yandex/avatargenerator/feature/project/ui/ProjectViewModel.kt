package com.yandex.avatargenerator.feature.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel экрана проекта: грузит по [projectId] диалог с ИИ через
 * [ChatRepository], держит черновик ввода и отправляет сообщения.
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

    fun onDraftChange(text: String) {
        _uiState.update { state ->
            if (state is ProjectUiState.Content) state.copy(draft = text) else state
        }
    }

    /**
     * Отправляет черновик и дописывает результат в конец ленты.
     *
     * Поле очищается сразу, до ответа репозитория, — иначе на время задержки
     * ввод «залипает» с уже отправленным текстом. Если отправка не удалась,
     * текст возвращается в поле, чтобы не пропасть без следа.
     */
    fun send() {
        val state = _uiState.value
        if (state !is ProjectUiState.Content) return
        val text = state.draft.trim()
        if (text.isEmpty()) return

        _uiState.update { if (it is ProjectUiState.Content) it.copy(draft = "") else it }
        viewModelScope.launch {
            // TODO: индикация неотправленного сообщения со статусом и повтором.
            chatRepository.sendMessage(projectId, text).fold(
                onSuccess = { message ->
                    _uiState.update { current ->
                        if (current is ProjectUiState.Content) {
                            current.copy(messages = current.messages + message)
                        } else {
                            current
                        }
                    }
                },
                onFailure = {
                    _uiState.update { current ->
                        if (current is ProjectUiState.Content && current.draft.isEmpty()) {
                            current.copy(draft = text)
                        } else {
                            current
                        }
                    }
                },
            )
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
