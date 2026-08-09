package com.yandex.avatargenerator.feature.profile.domain

import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.profile.domain.model.UserProfile

/**
 * Источник данных профиля пользователя.
 *
 * Проекты профиля отдаём тем же типом [FeedProject], что и лента, — карточка
 * ([com.yandex.avatargenerator.ui.components.ProjectCard]) и сетка
 * переиспользуются как есть.
 */
interface ProfileRepository {

    /** Профиль по id. Соответствует `GET /users/{id}`. */
    suspend fun getProfile(userId: String): UserProfile

    /** Проекты пользователя. Соответствует `GET /users/{id}/projects`. */
    suspend fun getUserProjects(userId: String): List<FeedProject>
}
