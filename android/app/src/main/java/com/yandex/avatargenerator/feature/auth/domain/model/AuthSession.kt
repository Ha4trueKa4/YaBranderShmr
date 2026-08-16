package com.yandex.avatargenerator.feature.auth.domain.model

/**
 * Результат входа или регистрации (`AuthResponse`).
 *
 * [tokenType] не информационное поле: репозиторий сверяет его с `Bearer` перед
 * сохранением сессии — заголовок собирается строкой, и любой другой тип означал
 * бы разошедшийся контракт.
 */
data class AuthSession(
    val accessToken: String,
    val tokenType: String,
    val user: AuthUser,
)
