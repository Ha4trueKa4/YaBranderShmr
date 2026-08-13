package com.yandex.avatargenerator.feature.auth.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ `POST /auth/login` и `POST /auth/register`. Поля не nullable —
 * единственная схема в swagger с полным `required`.
 */
@Serializable
data class AuthResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("user") val user: UserShortDto,
)
