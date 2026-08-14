package com.yandex.avatargenerator.feature.auth.domain.model

/**
 * Авторизованный пользователь (`AuthResponse.user`).
 *
 * Здесь `nickname`, а `GET /users/{id}` отдаёт `name` — расхождение в контракте,
 * см. `API.md`.
 */
data class AuthUser(
    val id: String,
    val email: String,
    val nickname: String,
)
