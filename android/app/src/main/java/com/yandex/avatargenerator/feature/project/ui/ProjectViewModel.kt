package com.yandex.avatargenerator.feature.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.project.domain.ChatRepository
import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel экрана проекта: грузит по [projectId] сам проект (название, видимость)
 * через [ProjectRepository] и диалог с ИИ через [ChatRepository], держит черновик
 * ввода и отправляет сообщения.
 */
internal class ProjectViewModel(
    private val projectRepository: ProjectRepository,
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
            // Отдельно ошибку отправки не показываем: снекбара в дизайн-системе нет,
            // а сваливать весь экран в Error из-за одного сообщения — хуже. Чтобы
            // текст всё же не пропал, возвращаем его в поле ввода при провале.
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
                    // Пользователь мог начать набирать новое, пока шла отправка, — тогда
                    // старый текст не возвращаем, чтобы не перетереть ввод.
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

    private fun load() {
        _uiState.value = ProjectUiState.Loading
        viewModelScope.launch {
            // Проект и диалог грузим параллельно; репозитории отдают Result, а не
            // бросают. Провал любого из двух — общий Error (экран показывает повтор).
            val (projectResult, chatResult) = coroutineScope {
                val project = async { projectRepository.getProject(projectId) }
                val chat = async { chatRepository.getChat(projectId) }
                project.await() to chat.await()
            }

            val project = projectResult.getOrNull()
            val chat = chatResult.getOrNull()
            _uiState.value = if (project != null && chat != null) {
                ProjectUiState.Content(
                    title = project.name,
                    isPublic = project.isPublic,
                    messages = chat.messages,
                )
            } else {
                ProjectUiState.Error
            }
        }
    }
}
