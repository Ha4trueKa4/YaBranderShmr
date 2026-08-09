package com.yandex.avatargenerator.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.profile.domain.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel экрана профиля. Грузит профиль и его проекты по [userId], хранит
 * состояние подписки (кнопка Follow) локально до готовности бэкенда.
 */
class ProfileViewModel(
    private val repository: ProfileRepository,
    private val userId: String,
    private val isOwnProfile: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            _uiState.value = runCatching {
                val profile = repository.getProfile(userId)
                val projects = repository.getUserProjects(userId)
                ProfileUiState.Content(profile, projects, isOwnProfile)
            }.getOrElse { ProfileUiState.Error }
        }
    }

    /**
     * Переключает подписку на чужой профиль (оптимистично, без сети).
     * Соответствует `POST`/`DELETE /users/{id}/subscribe`.
     */
    fun toggleFollow() {
        _uiState.update { state ->
            if (state !is ProfileUiState.Content || state.isOwnProfile) return@update state
            val following = !state.profile.isFollowing
            val delta = if (following) 1 else -1
            state.copy(
                profile = state.profile.copy(
                    isFollowing = following,
                    followersCount = state.profile.followersCount + delta,
                ),
            )
        }
    }
}
