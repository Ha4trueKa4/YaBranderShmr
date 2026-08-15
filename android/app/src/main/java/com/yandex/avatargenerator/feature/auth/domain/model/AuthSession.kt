package com.yandex.avatargenerator.feature.auth.domain.model

/** Результат входа или регистрации (`AuthResponse`). */
data class AuthSession(
    val accessToken: String,
    val tokenType: String,
    val user: AuthUser,
)
