package com.yandex.avatargenerator.feature.auth.domain

import com.yandex.avatargenerator.feature.auth.domain.model.AuthSession

/**
 * Вход по email и паролю. Токен репозиторий сохраняет сам, наружу возвращается
 * только сессия.
 *
 * @throws AuthException.InvalidCredentials неверный email или пароль (401).
 * @throws AuthException.FieldRejected сервер отверг формат поля (400 или 422).
 */
internal class LoginUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): AuthSession =
        repository.login(email, password)
}
