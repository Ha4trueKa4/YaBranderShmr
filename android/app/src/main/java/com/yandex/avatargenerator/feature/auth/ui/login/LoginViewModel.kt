package com.yandex.avatargenerator.feature.auth.ui.login

import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.core.mvi.BaseViewModel
import com.yandex.avatargenerator.feature.auth.domain.LoginUseCase
import com.yandex.avatargenerator.feature.auth.domain.LoginWithYandexUseCase
import com.yandex.avatargenerator.feature.auth.domain.YandexAuthRequestFactory
import com.yandex.avatargenerator.feature.auth.ui.AuthField
import com.yandex.avatargenerator.feature.auth.ui.AuthFormError
import com.yandex.avatargenerator.feature.auth.ui.MaxAuthInputLength
import com.yandex.avatargenerator.feature.auth.ui.targetField
import com.yandex.avatargenerator.feature.auth.ui.toAuthFormError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * ViewModel экрана входа: держит поля формы и по [LoginIntent.OnSubmitClick]
 * логинит пользователя через [LoginUseCase]. Вход через Yandex ID — по
 * [LoginIntent.OnYandexClick] открывает экран согласия, а пришедший `code`
 * меняет на токен через [LoginWithYandexUseCase]. Токен репозиторий сохраняет
 * сам — сюда возвращается только результат, по которому шлётся
 * [LoginEffect.NavigateToApp].
 */
internal class LoginViewModel(
    private val login: LoginUseCase,
    private val loginWithYandex: LoginWithYandexUseCase,
    private val yandexAuthRequestFactory: YandexAuthRequestFactory,
) : BaseViewModel<LoginIntent, LoginState, LoginEffect>(
    LoginState(isYandexAvailable = yandexAuthRequestFactory.isConfigured()),
) {

    override suspend fun handleIntent(intent: LoginIntent, state: LoginState) {
        when (intent) {
            // Правка поля гасит ошибки: старое сообщение относилось к прошлому
            // вводу. Ввод обрезаем по [MaxAuthInputLength] — см. комментарий
            // у константы.
            is LoginIntent.OnEmailChange -> updateState {
                copy(
                    email = intent.value.take(MaxAuthInputLength),
                    emailError = null,
                    formError = null,
                )
            }

            is LoginIntent.OnPasswordChange -> updateState {
                copy(
                    password = intent.value.take(MaxAuthInputLength),
                    passwordError = null,
                    formError = null,
                )
            }

            LoginIntent.OnPasswordVisibilityToggle -> updateState {
                copy(isPasswordVisible = !isPasswordVisible)
            }

            LoginIntent.OnSubmitClick -> submit(state)
            LoginIntent.OnForgotPasswordClick -> sendEffect(LoginEffect.NavigateToPasswordRecovery)
            LoginIntent.OnRegisterClick -> sendEffect(LoginEffect.NavigateToRegister)

            LoginIntent.OnYandexClick -> if (!state.isBusy && state.isYandexAvailable) {
                updateState { copy(yandexAuth = yandexAuthRequestFactory.create(), formError = null) }
            }

            is LoginIntent.OnYandexCodeReceived -> exchangeYandexCode(intent.code)

            LoginIntent.OnYandexAuthDismissed -> updateState { copy(yandexAuth = null) }

            LoginIntent.OnYandexAuthFailed -> updateState {
                copy(yandexAuth = null, formError = AuthFormError.YandexFailed)
            }
        }
    }

    /**
     * Закрывает экран согласия и меняет `code` на токен. `isSubmitting`
     * поднимаем до `launch` по той же причине, что и в [submit]: цикл интентов
     * не приостанавливается, пока в канале есть следующий интент.
     */
    private fun exchangeYandexCode(code: String) {
        updateState { copy(yandexAuth = null, isSubmitting = true, formError = null) }
        viewModelScope.launch {
            try {
                loginWithYandex(code)
                updateState { copy(isSubmitting = false) }
                sendEffect(LoginEffect.NavigateToApp)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                updateState { applyError(failure) }
            }
        }
    }

    /**
     * Логинит и отправляет [LoginEffect.NavigateToApp].
     *
     * Пустые поля блокируют запуск (кнопка и так неактивна — это страховка на
     * случай гонки с последней рекомпозицией), повторный вызов во время запроса
     * второго запроса не шлёт. Сбой не роняет корутину: снимаем
     * [LoginState.isSubmitting] и показываем ошибку, чтобы можно было повторить.
     *
     * Запрос уходит отдельной корутиной, а не прямо в `handleIntent`: иначе
     * очередь интентов встала бы на всё время похода в сеть и набранный текст
     * перестал бы появляться в полях.
     *
     * `isSubmitting` поднимаем **до** `launch`, а не внутри: цикл интентов не
     * приостанавливается, пока в канале есть следующий интент, и второй тап по
     * кнопке успел бы увидеть `isSubmitting = false` и отправить второй запрос.
     */
    private fun submit(state: LoginState) {
        if (!state.canSubmit) return

        updateState {
            copy(
                isSubmitting = true,
                emailError = null,
                passwordError = null,
                formError = null,
            )
        }
        viewModelScope.launch {
            try {
                login(state.email, state.password)
                // Токен уже записан репозиторием — здесь остаётся только навигация.
                updateState { copy(isSubmitting = false) }
                sendEffect(LoginEffect.NavigateToApp)
            } catch (cancellation: CancellationException) {
                // Отмена корутины — не ошибка формы: пробрасываем, иначе экран
                // останется с бесконечным индикатором в кнопке.
                throw cancellation
            } catch (failure: Exception) {
                updateState { applyError(failure) }
            }
        }
    }
}

/**
 * Раскладывает ошибку по полю, которое её вызвало. Всё, что к полям входа не
 * привязано (в том числе ошибка про никнейм — с этого экрана она прийти не
 * может), показываем общим сообщением над кнопкой.
 */
private fun LoginState.applyError(failure: Throwable): LoginState {
    val error = failure.toAuthFormError()
    val field = error.targetField()
    return copy(
        isSubmitting = false,
        emailError = error.takeIf { field == AuthField.Email },
        passwordError = error.takeIf { field == AuthField.Password },
        formError = error.takeIf { field != AuthField.Email && field != AuthField.Password },
    )
}
