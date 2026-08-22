package com.yandex.avatargenerator.feature.auth.ui.register

import com.yandex.avatargenerator.core.mvi.BaseState
import com.yandex.avatargenerator.feature.auth.ui.AuthFormError

/**
 * Состояние экрана регистрации — то же, что у входа, плюс никнейм.
 *
 * В макете поле подписано «Full name», в API оно называется `nickname`;
 * внутри кода везде `nickname` (как в DTO), подпись на экране не меняем.
 */
internal data class RegisterState(
    val nickname: String = "",
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val nicknameError: AuthFormError? = null,
    val emailError: AuthFormError? = null,
    val passwordError: AuthFormError? = null,
    val formError: AuthFormError? = null,
) : BaseState {

    /** Минимальная валидация: длину и формат проверяет сервер. */
    val canSubmit: Boolean
        get() = nickname.isNotBlank() && email.isNotBlank() && password.isNotBlank() && !isSubmitting
}
