package com.yandex.avatargenerator.feature.profile.data

import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.feature.profile.data.dto.UserProfileResponseDto
import com.yandex.avatargenerator.feature.profile.domain.ProfileRepository
import com.yandex.avatargenerator.feature.profile.domain.model.ProfileDetails
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.encodeURLPathPart

/**
 * Профиль через API (`GET /users/{id}`). Инъектит готовый [HttpClient] (свой не
 * собирает), вызов заворачивает в [apiCall] — наружу летит только доменная
 * [com.yandex.avatargenerator.core.domain.NetworkException]. `Bearer`
 * подставляет клиент сам; для чужого профиля он нужен, чтобы бэкенд заполнил
 * `is_subscribed` (без него состояние кнопки Follow неизвестно).
 */
class NetworkProfileRepository(
    private val client: HttpClient,
) : ProfileRepository {

    override suspend fun getProfile(userId: String): ProfileDetails = apiCall {
        client.get("users/${userId.encodeURLPathPart()}").body<UserProfileResponseDto>()
    }.toDomain(userId)
}
