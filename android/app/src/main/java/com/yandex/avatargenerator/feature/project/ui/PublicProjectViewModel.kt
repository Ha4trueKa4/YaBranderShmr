package com.yandex.avatargenerator.feature.project.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.project.domain.PublicProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel экрана публичного проекта: грузит проект по [projectId] через
 * [PublicProjectRepository] и держит состояние кнопки подписки.
 */
internal class PublicProjectViewModel(
    private val repository: PublicProjectRepository,
    private val projectId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PublicProjectUiState>(PublicProjectUiState.Loading)
    val uiState: StateFlow<PublicProjectUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    /** Повтор после ошибки. */
    fun retry() = load()

    /**
     * Переключает подписку на автора локально, без сети.
     *
     * Экран целиком живёт на [com.yandex.avatargenerator.feature.project.data
     * .MockPublicProjectRepository] — бэкенд по `GET /projects/{id}` пока не
     * отдаёт ни автора, ни его реальный id. Дёргать `POST/DELETE
     * /users/{id}/subscribe` с mock-id бессмысленно (сервер вернёт ошибку),
     * поэтому сетевую подписку подключим здесь вместе с сетевой витриной; сейчас
     * достаточно смены вида кнопки Follow/Following.
     */
    fun toggleFollow() {
        _uiState.update { state ->
            if (state !is PublicProjectUiState.Content) return@update state
            val author = state.project.author
            state.copy(project = state.project.copy(author = author.copy(isFollowing = !author.isFollowing)))
        }
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
