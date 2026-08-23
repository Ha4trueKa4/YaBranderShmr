package com.yandex.avatargenerator.feature.auth.ui.login

import com.yandex.avatargenerator.core.mvi.BaseEffect

/**
 * Одноразовые события экрана входа — вся навигация отсюда. Отдаются через канал,
 * а не через [LoginState], чтобы переход не повторялся на каждой рекомпозиции.
 */
internal sealed interface LoginEffect : BaseEffect {

    /** Вход прошёл, токен сохранён — можно уходить в приложение. */
    data object NavigateToApp : LoginEffect

    data object NavigateToRegister : LoginEffect

    data object NavigateToPasswordRecovery : LoginEffect
}
