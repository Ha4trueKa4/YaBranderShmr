package com.yandex.avatargenerator.feature.auth.ui.login

import com.yandex.avatargenerator.core.mvi.BaseIntent

/** Всё, что пользователь может сделать на экране входа. */
internal sealed interface LoginIntent : BaseIntent {

    data class OnEmailChange(val value: String) : LoginIntent

    data class OnPasswordChange(val value: String) : LoginIntent

    data object OnPasswordVisibilityToggle : LoginIntent

    data object OnSubmitClick : LoginIntent

    /** Восстановление пароля: ручки в API нет, экран только уводит наружу. */
    data object OnForgotPasswordClick : LoginIntent

    data object OnRegisterClick : LoginIntent
}
