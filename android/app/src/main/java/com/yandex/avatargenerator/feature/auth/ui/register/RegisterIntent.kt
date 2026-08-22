package com.yandex.avatargenerator.feature.auth.ui.register

import com.yandex.avatargenerator.core.mvi.BaseIntent

/** Всё, что пользователь может сделать на экране регистрации. */
internal sealed interface RegisterIntent : BaseIntent {

    data class OnNicknameChange(val value: String) : RegisterIntent

    data class OnEmailChange(val value: String) : RegisterIntent

    data class OnPasswordChange(val value: String) : RegisterIntent

    data object OnPasswordVisibilityToggle : RegisterIntent

    data object OnSubmitClick : RegisterIntent

    data object OnLoginClick : RegisterIntent
}
