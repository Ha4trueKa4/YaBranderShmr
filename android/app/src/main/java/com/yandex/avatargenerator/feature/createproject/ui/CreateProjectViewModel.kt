package com.yandex.avatargenerator.feature.createproject.ui

import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.core.mvi.BaseViewModel
import com.yandex.avatargenerator.feature.project.domain.GenerationModelRepository
import com.yandex.avatargenerator.feature.project.domain.ProjectRepository
import com.yandex.avatargenerator.feature.project.ui.ModelPickerState
import com.yandex.avatargenerator.feature.project.ui.resolveSelected
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
 *
 * Модель генерации выбирается тем же пикером, что и в чате проекта
 * ([GenerationModelRepository]): выбор глобальный, один на приложение, а
 * справочник тянется лениво — при первом открытии шторки. Первичная генерация
 * уходит на выбранной здесь модели.
 */
internal class CreateProjectViewModel(
    private val repository: ProjectRepository,
    private val generationModelRepository: GenerationModelRepository,
) : BaseViewModel<CreateProjectIntent, CreateProjectState, CreateProjectEffect>(
    CreateProjectState(),
) {

    init {
        // Чип рисует имя из сохранённого id без похода в сеть — сам справочник
        // ждёт первого открытия шторки.
        viewModelScope.launch {
            val selectedModelId = generationModelRepository.getSelectedModelId()
            updateModelPicker { copy(selectedModelId = selectedModelId) }
        }
    }

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

            CreateProjectIntent.OnModelPickerOpen -> openModelPicker(state.modelPicker)
            CreateProjectIntent.OnModelPickerDismiss -> updateModelPicker {
                copy(isSheetVisible = false)
            }

            is CreateProjectIntent.OnModelSelect -> selectModel(intent.id)
            CreateProjectIntent.OnModelsRetry -> if (!state.modelPicker.isLoading) loadModels()
        }
    }

    /**
     * Создаёт проект и отправляет [CreateProjectEffect.NavigateToProject] с его
     * настоящим id и id запущенной первичной генерации — ждать её будет уже
     * экран проекта, форма для этого живёт слишком мало.
     *
     * Пустой промпт блокирует запуск (кнопка и так неактивна — это страховка на
     * случай гонки с последней рекомпозицией) и заодно экономит заведомо
     * ошибочный запрос: пустой промпт бэкенд отвергает 400.
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
                model = state.modelPicker.selectedModelId,
            )
                .onSuccess { creation ->
                    updateState { copy(isSubmitting = false) }
                    sendEffect(
                        CreateProjectEffect.NavigateToProject(
                            projectId = creation.project.id,
                            jobId = creation.jobId,
                        ),
                    )
                }
                .onFailure {
                    updateState { copy(isSubmitting = false, hasError = true) }
                }
        }
    }

    /** Открывает шторку и при первом открытии тянет справочник. */
    private fun openModelPicker(picker: ModelPickerState) {
        updateModelPicker { copy(isSheetVisible = true) }
        if (picker.models.isEmpty() && !picker.isLoading) loadModels()
    }

    /**
     * Запоминает выбор и закрывает шторку. Состояние правим сразу, не дожидаясь
     * записи в настройки: проект всё равно создастся с тем `id`, что лежит в
     * состоянии, а запись — фоновая деталь.
     */
    private fun selectModel(id: String) {
        updateModelPicker { copy(selectedModelId = id, isSheetVisible = false) }
        viewModelScope.launch { generationModelRepository.selectModel(id) }
    }

    /**
     * Тянет справочник моделей.
     *
     * Пустой список от ошибки не отличаем: выбирать всё равно не из чего, а
     * шторке с пустым списком нечего показать, кроме той же ошибки с ретраем.
     * Создание проекта ни то, ни другое не блокирует — в теле уедет сохранённый
     * `id`, и разбираться с ним будет сервер.
     */
    private fun loadModels() {
        updateModelPicker { copy(isLoading = true, hasError = false) }
        viewModelScope.launch {
            val models = generationModelRepository.getAvailableModels().getOrNull().orEmpty()
            if (models.isEmpty()) {
                updateModelPicker { copy(isLoading = false, hasError = true) }
                return@launch
            }

            val selected = state.value.modelPicker.selectedModelId
            val resolved = models.resolveSelected(selected)
            updateModelPicker {
                copy(
                    models = models,
                    selectedModelId = resolved,
                    isLoading = false,
                    hasError = false,
                )
            }
            // Расхождение перезаписываем, чтобы оно не всплыло ещё раз.
            if (resolved != selected) generationModelRepository.selectModel(resolved)
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

    private fun updateModelPicker(block: ModelPickerState.() -> ModelPickerState) {
        updateState { copy(modelPicker = modelPicker.block()) }
    }
}
