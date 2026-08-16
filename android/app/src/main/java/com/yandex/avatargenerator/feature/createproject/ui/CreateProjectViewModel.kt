package com.yandex.avatargenerator.feature.createproject.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel формы создания проекта: держит поля ввода, валидирует их и по
 * запросу создаёт проект через `POST /projects` ([ProjectRepository] из
 * `feature/project` — своего репозитория у экрана нет, см. KDoc метода),
 * после чего шлёт событие навигации на экран проекта.
 */
internal class CreateProjectViewModel(
    private val repository: ProjectRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateProjectUiState())
    val uiState: StateFlow<CreateProjectUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreateProjectEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, hasError = false) }

    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    /**
     * Создаёт проект и отправляет [CreateProjectEvent.ProjectCreated] с его
     * настоящим id. Пустое название блокирует запуск (кнопка и так неактивна —
     * это страховка на случай гонки с последней рекомпозицией) и заодно
     * экономит заведомо ошибочный запрос: пустое имя бэкенд отвергает 400.
     *
     * Репозиторий не бросает, а отдаёт [Result] — своего `runCatching` здесь
     * не нужно. Ошибка не роняет корутину: снимаем
     * [CreateProjectUiState.isSubmitting] и показываем ошибку, введённые поля
     * остаются на месте, чтобы пользователь мог повторить.
     */
    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, hasError = false) }
            repository.createProject(state.name.trim(), state.description.trim())
                .onSuccess { project ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    _events.send(CreateProjectEvent.ProjectCreated(project.id))
                }
                .onFailure {
                    _uiState.update { it.copy(isSubmitting = false, hasError = true) }
                }
        }
    }
}
