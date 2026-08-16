package com.yandex.avatargenerator.feature.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.project.domain.PublicProjectRepository
import com.yandex.avatargenerator.feature.subscription.domain.SubscriptionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel экрана публичного проекта: грузит проект по [projectId] через
 * [PublicProjectRepository] и держит состояние кнопки подписки на автора.
 */
internal class PublicProjectViewModel(
    private val repository: PublicProjectRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val projectId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PublicProjectUiState>(PublicProjectUiState.Loading)
    val uiState: StateFlow<PublicProjectUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Повтор после ошибки. */
    fun retry() = load()

    private var followJob: Job? = null

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
        viewModelScope.launch {
            _uiState.value = repository.getPublicProject(projectId).fold(
                onSuccess = { PublicProjectUiState.Content(it) },
                onFailure = { PublicProjectUiState.Error },
            )
        }
    }
}
