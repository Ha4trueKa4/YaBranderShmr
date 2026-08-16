package com.yandex.avatargenerator.feature.profile.domain

import com.yandex.avatargenerator.feature.profile.domain.model.ProfileDetails

/**
 * Источник данных профиля пользователя.
 *
 * `GET /users/{id}` возвращает профиль и его проекты в одном ответе, поэтому и
 * здесь это один вызов — [ProfileDetails].
 */
interface ProfileRepository {

    /** Профиль и его проекты по id. Соответствует `GET /users/{id}`. */
    suspend fun getProfile(userId: String): ProfileDetails
}
