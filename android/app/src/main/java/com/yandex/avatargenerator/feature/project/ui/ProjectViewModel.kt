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
     * ввод «залипает» с уже отправленным текстом.
     */
    fun send() {
        val state = _uiState.value
        if (state !is ProjectUiState.Content) return
        val text = state.draft.trim()
        if (text.isEmpty()) return

        _uiState.value = state.copy(draft = "")
        viewModelScope.launch {
            // Ошибку отправки пока проглатываем: показывать её нечем (нет снекбара
            // в дизайн-системе), а сваливать весь экран в Error из-за одного
            // сообщения — хуже. TODO: индикация неотправленного сообщения.
            val message = runCatching { repository.sendMessage(projectId, text) }.getOrNull()
                ?: return@launch
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
