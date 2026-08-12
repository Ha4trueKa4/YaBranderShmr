package com.yandex.avatargenerator.feature.profile.ui

import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.profile.domain.model.UserProfile

/** Состояние экрана профиля. */
sealed interface ProfileUiState {

    data object Loading : ProfileUiState

    data object Error : ProfileUiState

    /**
     * @param isOwnProfile свой профиль (кнопка «Edit profile») или чужой
     *   (кнопка «Follow»/«Following»).
     */
    data class Content(
        val profile: UserProfile,
        val projects: List<FeedProject>,
        val isOwnProfile: Boolean,
    ) : ProfileUiState
}
