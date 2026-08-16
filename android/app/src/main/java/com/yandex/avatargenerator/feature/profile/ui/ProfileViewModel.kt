package com.yandex.avatargenerator.feature.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.profile.domain.ProfileRepository
import com.yandex.avatargenerator.feature.subscription.domain.SubscriptionRepository
import kotlinx.coroutines.Job
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
    private val subscriptionRepository: SubscriptionRepository,
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
                val details = repository.getProfile(userId)
                ProfileUiState.Content(details.profile, details.projects, isOwnProfile)
            }.getOrElse { ProfileUiState.Error }
        }
    }

    private var followJob: Job? = null

    /**
     * Переключает подписку на чужой профиль через `POST`/`DELETE
     * /users/{id}/subscribe`. Обновляем UI оптимистично и откатываем, если
     * запрос не прошёл.
     *
     * Пока запрос в полёте, повторные тапы игнорируем: без этого частые нажатия
     * запускали бы встречные subscribe/unsubscribe, и их откаты разъезжали бы
     * `isFollowing` и `followersCount`.
     */
    fun toggleFollow() {
        val current = _uiState.value
        if (current !is ProfileUiState.Content || current.isOwnProfile) return
        if (followJob?.isActive == true) return

        val following = !current.profile.isFollowing
        _uiState.update { applyFollow(it, following) }

        followJob = viewModelScope.launch {
            val result = if (following) {
                subscriptionRepository.subscribe(current.profile.id)
            } else {
                subscriptionRepository.unsubscribe(current.profile.id)
            }
            if (result.isFailure) {
                _uiState.update { applyFollow(it, !following) }
            }
        }
    }

    private fun applyFollow(state: ProfileUiState, following: Boolean): ProfileUiState {
        if (state !is ProfileUiState.Content) return state
        val delta = if (following) 1 else -1
        return state.copy(
            profile = state.profile.copy(
                isFollowing = following,
                followersCount = state.profile.followersCount + delta,
            ),
        )
    }
}
