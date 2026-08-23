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

    /** Тап по «Войти через Яндекс» — открыть экран согласия. */
    data object OnYandexClick : LoginIntent

    /** WebView согласия вернул `code` из редиректа — меняем его на токен. */
    data class OnYandexCodeReceived(val code: String) : LoginIntent

    /** Пользователь закрыл экран согласия — молча, без ошибки. */
    data object OnYandexAuthDismissed : LoginIntent

    /** Редирект пришёл с ошибкой или несовпавшим `state` — показать сбой. */
    data object OnYandexAuthFailed : LoginIntent
}
