package com.yandex.avatargenerator.feature.auth.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Тело `POST /auth/register`. В swagger все три поля обязательны. */
@Serializable
data class RegisterRequestDto(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("nickname") val nickname: String,
)
