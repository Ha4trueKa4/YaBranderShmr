package com.yandex.avatargenerator.feature.createproject.ui

import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.core.mvi.BaseViewModel
import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import kotlinx.coroutines.launch

/**
 * ViewModel формы создания проекта: держит поля ввода, валидирует их и по
 * [CreateProjectIntent.OnSubmitClick] создаёт проект через `POST /projects`
 * ([ProjectRepository] из `feature/project` — своего репозитория у экрана нет,
 * см. KDoc метода), после чего шлёт эффект навигации на экран проекта.
 */
internal class CreateProjectViewModel(
    private val repository: ProjectRepository,
) : BaseViewModel<CreateProjectIntent, CreateProjectState, CreateProjectEffect>(
    CreateProjectState(),
) {

    override suspend fun handleIntent(intent: CreateProjectIntent, state: CreateProjectState) {
        when (intent) {
            // Правка названия гасит ошибку: старое сообщение относилось к прошлой
            // попытке. Описание на ошибку не влияет — его бэкенд не отвергает.
            is CreateProjectIntent.OnNameChange -> updateState {
                copy(name = intent.value, hasError = false)
            }

            is CreateProjectIntent.OnDescriptionChange -> updateState {
                copy(description = intent.value)
            }

            CreateProjectIntent.OnSubmitClick -> submit(state)
            CreateProjectIntent.OnBackClick -> sendEffect(CreateProjectEffect.NavigateBack)
        }
    }

    /**
     * Создаёт проект и отправляет [CreateProjectEffect.NavigateToProject] с его
     * настоящим id. Пустое название блокирует запуск (кнопка и так неактивна —
     * это страховка на случай гонки с последней рекомпозицией) и заодно
     * экономит заведомо ошибочный запрос: пустое имя бэкенд отвергает 400.
     *
     * Репозиторий не бросает, а отдаёт [Result] — своего `runCatching` здесь
     * не нужно. Ошибка не роняет корутину: снимаем
     * [CreateProjectState.isSubmitting] и показываем ошибку, введённые поля
     * остаются на месте, чтобы пользователь мог повторить.
     *
     * Запрос уходит отдельной корутиной, а не прямо в `handleIntent`: иначе
     * очередь интентов встала бы на всё время генерации и набранный текст
     * перестал бы появляться в полях.
     *
     * `isSubmitting` поднимаем **до** `launch`, а не внутри: цикл интентов не
     * приостанавливается, пока в канале есть следующий интент, и второй тап по
     * кнопке успел бы увидеть `isSubmitting = false` и создать второй проект.
     */
    private fun submit(state: CreateProjectState) {
        if (!state.canSubmit) return

        updateState { copy(isSubmitting = true, hasError = false) }
        viewModelScope.launch {
            repository.createProject(state.name.trim(), state.description.trim())
                .onSuccess { project ->
                    updateState { copy(isSubmitting = false) }
                    sendEffect(CreateProjectEffect.NavigateToProject(project.id))
                }
                .onFailure {
                    updateState { copy(isSubmitting = false, hasError = true) }
                }
        }
    }
}
