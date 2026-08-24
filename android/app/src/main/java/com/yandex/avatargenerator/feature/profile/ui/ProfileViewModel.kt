package com.yandex.avatargenerator.feature.profile.ui

import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.core.mvi.BaseViewModel
import com.yandex.avatargenerator.feature.profile.domain.ProfileRepository
import com.yandex.avatargenerator.feature.subscription.domain.SubscriptionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * ViewModel экрана профиля. Грузит профиль и его проекты по [userId] и держит
 * состояние подписки (кнопка Follow).
 *
 * `userId`, `isOwnProfile` и флаги видимости кнопок известны только при открытии
 * экрана — приходят конструктором (Koin `parametersOf`) и сразу ложатся
 * в начальное состояние.
 */
class ProfileViewModel(
    private val repository: ProfileRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val userId: String,
    isOwnProfile: Boolean,
    showBackButton: Boolean = false,
    showSettingsButton: Boolean = false,
) : BaseViewModel<ProfileIntent, ProfileState, ProfileEffect>(
    ProfileState(
        isOwnProfile = isOwnProfile,
        showBackButton = showBackButton,
        showSettingsButton = showSettingsButton,
    ),
) {

    init {
        load()
    }

    override suspend fun handleIntent(intent: ProfileIntent, state: ProfileState) {
        when (intent) {
            ProfileIntent.OnRetryClick -> load()

            // Одна кнопка на два сценария: ветвление «свой/чужой» — логика, ей
            // место здесь, а не в разметке.
            ProfileIntent.OnActionClick -> if (state.isOwnProfile) {
                sendEffect(ProfileEffect.OpenEditProfile)
            } else {
                toggleFollow(state)
            }

            is ProfileIntent.OnProjectClick ->
                sendEffect(ProfileEffect.OpenProject(intent.projectId))

            ProfileIntent.OnFollowersClick -> sendEffect(ProfileEffect.OpenFollowers(userId))
            ProfileIntent.OnFollowingClick -> sendEffect(ProfileEffect.OpenFollowing(userId))
            ProfileIntent.OnSettingsClick -> sendEffect(ProfileEffect.OpenSettings)
            ProfileIntent.OnBackClick -> sendEffect(ProfileEffect.NavigateBack)
        }
    }

    /**
     * Переключает подписку на чужой профиль через `POST`/`DELETE
     * /users/{id}/subscribe`. Обновляем UI оптимистично и откатываем, если
     * запрос не прошёл.
     *
     * Пока запрос в полёте, повторные тапы игнорируем: без этого частые нажатия
     * запускали бы встречные subscribe/unsubscribe, и их откаты разъезжали бы
     * `isFollowing` и `followersCount`. Флаг поднимаем **до** `launch` — цикл
     * интентов не приостанавливается, пока в канале есть следующий интент.
     */
    private fun toggleFollow(state: ProfileState) {
        val profile = state.profile ?: return
        if (state.isFollowInFlight) return

        val following = !profile.isFollowing
        updateState { copy(isFollowInFlight = true).applyFollow(following) }

        viewModelScope.launch {
            val result = if (following) {
                subscriptionRepository.subscribe(profile.id)
            } else {
                subscriptionRepository.unsubscribe(profile.id)
            }
            updateState {
                val settled = copy(isFollowInFlight = false)
                if (result.isFailure) settled.applyFollow(!following) else settled
            }
        }
    }

    private fun ProfileState.applyFollow(following: Boolean): ProfileState {
        val current = profile ?: return this
        val delta = if (following) 1 else -1
        return copy(
            profile = current.copy(
                isFollowing = following,
                followersCount = current.followersCount + delta,
            ),
        )
    }

    /**
     * Грузит профиль отдельной корутиной, а не прямо в `handleIntent`: иначе
     * очередь интентов встала бы на всё время похода в сеть.
     */
    private fun load() {
        updateState { copy(isLoading = true, hasError = false) }
        viewModelScope.launch {
            try {
                val details = repository.getProfile(userId)
                updateState {
                    copy(
                        isLoading = false,
                        profile = details.profile,
                        projects = details.projects,
                        hasError = false,
                    )
                }
            } catch (cancellation: CancellationException) {
                // Отмена корутины — не ошибка загрузки: пробрасываем, иначе экран
                // остался бы с состоянием ошибки поверх актуальных данных.
                throw cancellation
            } catch (failure: Exception) {
                updateState { copy(isLoading = false, hasError = true) }
            }
        }
    }
}
