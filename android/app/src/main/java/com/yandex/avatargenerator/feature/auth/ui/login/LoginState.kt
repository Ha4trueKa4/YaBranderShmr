package com.yandex.avatargenerator.feature.auth.ui.login

import com.yandex.avatargenerator.core.mvi.BaseState
import com.yandex.avatargenerator.feature.auth.domain.model.YandexAuthRequest
import com.yandex.avatargenerator.feature.auth.ui.AuthFormError

/**
 * Состояние экрана входа: поля формы и результат последней попытки.
 *
 * Ошибок три штуки, а не одна: сервер умеет сказать, какое поле не устроило,
 * и подпись должна оказаться под ним. [formError] — то, что к конкретному
 * полю не привязано (неверная пара логин/пароль, сеть, сбой Yandex ID),
 * показывается над кнопкой.
 *
 * @property isSubmitting идёт запрос (вход по паролю или обмен `code` Yandex ID
 *  на токен) — блокирует кнопки и меняет текст основной.
 * @property yandexAuth не `null` — открыт WebView согласия Yandex ID; экран
 *  показывает его поверх формы, пока не придёт `code` или пользователь не закроет.
 * @property isYandexAvailable настроен ли вход через Yandex ID (задан `client_id`);
 *  если нет — кнопку не показываем, чтобы не открывать заведомо сломанный экран.
 */
internal data class LoginState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isSubmitting: Boolean = false,
    val emailError: AuthFormError? = null,
    val passwordError: AuthFormError? = null,
    val formError: AuthFormError? = null,
    val yandexAuth: YandexAuthRequest? = null,
    val isYandexAvailable: Boolean = false,
) : BaseState {

    /** Идёт вход или открыт экран согласия — новые действия не запускаем. */
    val isBusy: Boolean get() = isSubmitting || yandexAuth != null

    /** Минимальная валидация: правила пароля и email проверяет сервер. */
    val canSubmit: Boolean get() = email.isNotBlank() && password.isNotBlank() && !isBusy
}
