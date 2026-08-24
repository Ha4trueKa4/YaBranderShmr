package com.yandex.avatargenerator.feature.createproject.ui

import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.core.mvi.BaseViewModel
import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import kotlinx.coroutines.launch

/**
 * Имя, под которым проект уедет на сервер, если пользователь не ввёл своё.
 * В `strings.xml` не выносим сознательно: имя уезжает на сервер и там
 * застывает, локализовать его на момент создания смысла нет.
 */
private const val DefaultProjectName = "Untitled project"

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
            // Правка промпта гасит ошибку: старое сообщение относилось к прошлой
            // попытке, а отвергает сервер именно промпт. Название и описание на
            // ошибку не влияют — их бэкенд принимает любыми.
            is CreateProjectIntent.OnPromptChange -> updateState {
                copy(prompt = limitCreatePrompt(intent.value), hasError = false)
            }

            is CreateProjectIntent.OnNameChange -> updateState {
                copy(name = intent.value)
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
     * настоящим id. Пустой промпт блокирует запуск (кнопка и так неактивна —
     * это страховка на случай гонки с последней рекомпозицией) и заодно
     * экономит заведомо ошибочный запрос: пустой промпт бэкенд отвергает 400.
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
            repository.createProject(
                name = effectiveName(state),
                description = state.description.trim(),
                prompt = state.prompt.trim(),
            )
                .onSuccess { creation ->
                    updateState { copy(isSubmitting = false) }
                    sendEffect(CreateProjectEffect.NavigateToProject(creation.project.id))
                }
                .onFailure {
                    updateState { copy(isSubmitting = false, hasError = true) }
                }
        }
    }

    /**
     * Название для запроса: введённое пользователем либо [DefaultProjectName].
     *
     * Пустое имя бэкенд примет и запишет как `""` — проект встал бы безымянным
     * и в списке чатов, и в шапке своего экрана. Учить чужие экраны рисовать
     * плейсхолдер вместо пустого имени — это правки в чужих фичах; подмена
     * здесь дешевле и живёт в одном месте.
     */
    private fun effectiveName(state: CreateProjectState): String =
        state.name.trim().ifBlank { DefaultProjectName }
}
