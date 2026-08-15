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
 * ViewModel экрана проекта: грузит диалог по [projectId], держит черновик ввода
 * и отправляет сообщения через [ChatRepository].
 */
internal class ProjectViewModel(
    private val repository: ChatRepository,
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
            // Отдельно ошибку отправки не показываем: снекбара в дизайн-системе нет,
            // а сваливать весь экран в Error из-за одного сообщения — хуже. Чтобы
            // текст всё же не пропал, возвращаем его в поле ввода (см. ниже).
            // TODO: индикация неотправленного сообщения со статусом и повтором.
            val message = runCatching { repository.sendMessage(projectId, text) }.getOrNull()
            if (message == null) {
                // Пользователь мог начать набирать новое, пока шла отправка, — тогда
                // старый текст не возвращаем, чтобы не перетереть ввод.
                _uiState.update { current ->
                    if (current is ProjectUiState.Content && current.draft.isEmpty()) {
                        current.copy(draft = text)
                    } else {
                        current
                    }
                }
                return@launch
            }
            _uiState.update { current ->
                if (current is ProjectUiState.Content) {
                    current.copy(messages = current.messages + message)
                } else {
                    current
                }
            }
        }
    }

    private fun load() {
        _uiState.value = ProjectUiState.Loading
        viewModelScope.launch {
            _uiState.value = runCatching { repository.getChat(projectId) }.fold(
                onSuccess = { chat ->
                    ProjectUiState.Content(title = chat.title, messages = chat.messages)
                },
                onFailure = { ProjectUiState.Error },
            )
        }
    }
}
