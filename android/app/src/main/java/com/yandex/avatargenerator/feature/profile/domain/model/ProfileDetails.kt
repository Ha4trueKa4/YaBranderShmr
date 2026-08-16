package com.yandex.avatargenerator.feature.profile.domain.model

import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject

/**
 * Профиль и его проекты вместе: `GET /users/{id}` отдаёт и то, и другое одним
 * ответом, поэтому экрану не нужен второй запрос.
 *
 * Проекты — тем же типом [FeedProject], что и лента: карточка
 * ([com.yandex.avatargenerator.ui.components.ProjectCard]) и сетка
 * переиспользуются как есть.
 */
data class ProfileDetails(
    val profile: UserProfile,
    val projects: List<FeedProject>,
)
