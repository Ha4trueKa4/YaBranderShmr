package com.yandex.avatargenerator.feature.auth.ui

/**
 * Состояние экрана регистрации — то же, что у входа, плюс никнейм.
 *
 * В макете поле подписано «Full name», в API оно называется `nickname`;
 * внутри кода везде `nickname` (как в DTO), подпись на экране не меняем.
 */
internal data class RegisterUiState(
    val nickname: String = "",
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val nicknameError: AuthFormError? = null,
    val emailError: AuthFormError? = null,
    val passwordError: AuthFormError? = null,
    val formError: AuthFormError? = null,
) {
    /** Минимальная валидация: длину и формат проверяет сервер. */
    val canSubmit: Boolean
        get() = nickname.isNotBlank() && email.isNotBlank() && password.isNotBlank() && !isSubmitting
}

/**
 * Одноразовое событие экрана. Отдаётся через канал, а не через [RegisterUiState],
 * чтобы переход не повторялся на каждой рекомпозиции.
 */
internal sealed interface RegisterEvent {

    /** Аккаунт создан, токен сохранён — можно уходить в приложение. */
    data object Registered : RegisterEvent
}
