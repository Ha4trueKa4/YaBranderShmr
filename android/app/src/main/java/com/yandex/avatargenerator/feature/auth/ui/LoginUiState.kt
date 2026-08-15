package com.yandex.avatargenerator.feature.auth.ui

/**
 * Состояние экрана входа: поля формы и результат последней попытки.
 *
 * Ошибок три штуки, а не одна: сервер умеет сказать, какое поле не устроило,
 * и подпись должна оказаться под ним. [formError] — то, что к конкретному
 * полю не привязано (неверная пара логин/пароль, сеть), показывается над кнопкой.
 *
 * @property isSubmitting идёт запрос — блокирует кнопку и меняет её текст.
 */
internal data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val emailError: AuthFormError? = null,
    val passwordError: AuthFormError? = null,
    val formError: AuthFormError? = null,
) {
    /** Минимальная валидация: правила пароля и email проверяет сервер. */
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !isSubmitting
}

/**
 * Одноразовое событие экрана. Отдаётся через канал, а не через [LoginUiState],
 * чтобы переход не повторялся на каждой рекомпозиции.
 */
internal sealed interface LoginEvent {

    /** Вход прошёл, токен сохранён — можно уходить в приложение. */
    data object SignedIn : LoginEvent
}
