package com.yandex.avatargenerator.feature.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.project.domain.ProjectExporter
import com.yandex.avatargenerator.feature.project.domain.PublicProjectRepository
import com.yandex.avatargenerator.feature.project.domain.model.ExportFormat
import com.yandex.avatargenerator.feature.subscription.domain.SubscriptionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel экрана публичного проекта: грузит проект по [projectId] через
 * [PublicProjectRepository], держит состояние кнопки подписки и запускает
 * выгрузку превью в PNG/SVG ([ProjectExporter]).
 */
internal class PublicProjectViewModel(
    private val repository: PublicProjectRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val exporter: ProjectExporter,
    private val projectId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PublicProjectUiState>(PublicProjectUiState.Loading)
    val uiState: StateFlow<PublicProjectUiState> = _uiState.asStateFlow()

    // Результат выгрузки — одноразовое событие для тоста; Channel, чтобы поворот
    // экрана не проигрывал его повторно (в отличие от StateFlow).
    private val _exportEvents = Channel<ExportEvent>(Channel.BUFFERED)
    val exportEvents: Flow<ExportEvent> = _exportEvents.receiveAsFlow()

    init {
        load()
    }

    /** Повтор после ошибки. */
    fun retry() = load()

    private var followJob: Job? = null
    private var exportJob: Job? = null
    private var loadJob: Job? = null

    /**
     * Тихая перезагрузка при возврате на экран: скелетон не показываем, при ошибке
     * оставляем текущее содержимое. Пока экран грузится впервые или лежит в ошибке
     * — обновлять нечего ([load]); а с подпиской в полёте не лезем, чтобы не
     * затереть оптимистичное состояние кнопки ответом сервера.
     */
    fun refresh() {
        if (_uiState.value !is PublicProjectUiState.Content) return
        if (followJob?.isActive == true) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getPublicProject(projectId).onSuccess {
                _uiState.value = PublicProjectUiState.Content(it)
            }
        }
    }

    /**
     * Сохраняет превью проекта в «Загрузки» в выбранном [format]. Пока файл
     * пишется — повторные тапы игнорируем. Проект без превью выгружать нечего.
     */
    fun export(format: ExportFormat) {
        val content = _uiState.value as? PublicProjectUiState.Content ?: return
        if (exportJob?.isActive == true) return

        val svgUrl = content.project.previewUrl
        if (svgUrl.isNullOrBlank()) {
            _exportEvents.trySend(ExportEvent.NothingToExport)
            return
        }

        exportJob = viewModelScope.launch {
            val result = exporter.export(svgUrl, content.project.title, format)
            _exportEvents.send(
                if (result.isSuccess) ExportEvent.Success(format) else ExportEvent.Failure(format),
            )
        }
    }

    /**
     * Переключает подписку на автора через `POST`/`DELETE /users/{id}/subscribe`.
     * Обновляем UI оптимистично и откатываем, если запрос не прошёл; пока запрос
     * в полёте — повторные тапы игнорируем, чтобы встречные subscribe/unsubscribe
     * не разъезжались с откатами.
     *
     * Автор без id (профиль не подгрузился) подписать нельзя — тап игнорируем.
     */
    fun toggleFollow() {
        val current = _uiState.value
        if (current !is PublicProjectUiState.Content) return
        val authorId = current.project.author.id
        if (authorId.isEmpty()) return
        if (followJob?.isActive == true) return

        val following = !current.project.author.isFollowing
        // Сбрасываем тихую перезагрузку, если она в полёте: её ответ мог быть собран
        // до подписки и, придя позже, откатил бы оптимистичное состояние кнопки.
        loadJob?.cancel()
        _uiState.update { applyFollow(it, following) }

        followJob = viewModelScope.launch {
            val result = if (following) {
                subscriptionRepository.subscribe(authorId)
            } else {
                subscriptionRepository.unsubscribe(authorId)
            }
            if (result.isFailure) {
                _uiState.update { applyFollow(it, !following) }
            }
        }
    }

    private fun applyFollow(state: PublicProjectUiState, following: Boolean): PublicProjectUiState {
        if (state !is PublicProjectUiState.Content) return state
        val author = state.project.author
        return state.copy(project = state.project.copy(author = author.copy(isFollowing = following)))
    }

    private fun load() {
        _uiState.value = PublicProjectUiState.Loading
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = repository.getPublicProject(projectId).fold(
                onSuccess = { PublicProjectUiState.Content(it) },
                onFailure = { PublicProjectUiState.Error },
            )
        }
    }
}
