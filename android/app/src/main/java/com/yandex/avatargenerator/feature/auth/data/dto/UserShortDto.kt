package com.yandex.avatargenerator.feature.auth.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `AuthResponse.user`. Все поля обязательны. */
@Serializable
data class UserShortDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("nickname") val nickname: String,
)
