package com.yandex.avatargenerator.feature.auth.domain

import com.yandex.avatargenerator.feature.auth.domain.model.AuthSession

/**
 * Регистрация аккаунта. Email реализация нормализует, никнейм обрезает — здесь
 * ввод не трогаем, чтобы правила жили в одном месте.
 *
 * @throws AuthException.FieldRejected поле не прошло проверку (400 или 422).
 * @throws AuthException.RegistrationConflict email или никнейм заняты (409).
 */
internal class RegisterUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        nickname: String,
    ): AuthSession = repository.register(email, password, nickname)
}
