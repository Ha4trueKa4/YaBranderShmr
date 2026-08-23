package com.yandex.avatargenerator.feature.auth.ui.login

import com.yandex.avatargenerator.core.mvi.BaseState
import com.yandex.avatargenerator.feature.auth.ui.AuthFormError

/**
 * Состояние экрана входа: поля формы и результат последней попытки.
 *
 * Ошибок три штуки, а не одна: сервер умеет сказать, какое поле не устроило,
 * и подпись должна оказаться под ним. [formError] — то, что к конкретному
 * полю не привязано (неверная пара логин/пароль, сеть), показывается над кнопкой.
 *
 * @property isSubmitting идёт запрос — блокирует кнопку и меняет её текст.
 */
internal data class LoginState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val emailError: AuthFormError? = null,
    val passwordError: AuthFormError? = null,
    val formError: AuthFormError? = null,
) : BaseState {

    /** Минимальная валидация: правила пароля и email проверяет сервер. */
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !isSubmitting
}
