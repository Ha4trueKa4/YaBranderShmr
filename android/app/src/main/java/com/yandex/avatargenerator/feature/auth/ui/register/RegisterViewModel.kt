package com.yandex.avatargenerator.feature.auth.ui.register

import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.core.mvi.BaseViewModel
import com.yandex.avatargenerator.feature.auth.domain.RegisterUseCase
import com.yandex.avatargenerator.feature.auth.ui.AuthField
import com.yandex.avatargenerator.feature.auth.ui.MaxAuthInputLength
import com.yandex.avatargenerator.feature.auth.ui.targetField
import com.yandex.avatargenerator.feature.auth.ui.toAuthFormError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * ViewModel экрана регистрации: держит поля формы и по
 * [RegisterIntent.OnSubmitClick] создаёт аккаунт через [RegisterUseCase]. Токен
 * репозиторий сохраняет сам — сюда возвращается только результат, по которому
 * шлётся [RegisterEffect.NavigateToApp].
 *
 * Правила длины пароля и никнейма на клиенте не проверяются: их решает сервер,
 * а экран показывает подсказку по коду ошибки. Единственное ограничение ввода —
 * технический потолок [MaxAuthInputLength].
 */
internal class RegisterViewModel(
    private val register: RegisterUseCase,
) : BaseViewModel<RegisterIntent, RegisterState, RegisterEffect>(RegisterState()) {

    override suspend fun handleIntent(intent: RegisterIntent, state: RegisterState) {
        when (intent) {
            // Правка поля гасит ошибки: старое сообщение относилось к прошлому
            // вводу. Ввод обрезаем по [MaxAuthInputLength] — см. комментарий
            // у константы.
            is RegisterIntent.OnNicknameChange -> updateState {
                copy(
                    nickname = intent.value.take(MaxAuthInputLength),
                    nicknameError = null,
                    formError = null,
                )
            }

            is RegisterIntent.OnEmailChange -> updateState {
                copy(
                    email = intent.value.take(MaxAuthInputLength),
                    emailError = null,
                    formError = null,
                )
            }

            is RegisterIntent.OnPasswordChange -> updateState {
                copy(
                    password = intent.value.take(MaxAuthInputLength),
                    passwordError = null,
                    formError = null,
                )
            }

            RegisterIntent.OnPasswordVisibilityToggle -> updateState {
                copy(isPasswordVisible = !isPasswordVisible)
            }

            RegisterIntent.OnSubmitClick -> submit(state)
            RegisterIntent.OnLoginClick -> sendEffect(RegisterEffect.NavigateToLogin)
        }
    }

    /**
     * Регистрирует и отправляет [RegisterEffect.NavigateToApp].
     *
     * Пустые поля блокируют запуск (кнопка и так неактивна — это страховка на
     * случай гонки с последней рекомпозицией), повторный вызов во время запроса
     * второго запроса не шлёт. Сбой не роняет корутину: снимаем
     * [RegisterState.isSubmitting] и показываем ошибку, чтобы можно было повторить.
     *
     * Запрос уходит отдельной корутиной, а не прямо в `handleIntent`: иначе
     * очередь интентов встала бы на всё время похода в сеть и набранный текст
     * перестал бы появляться в полях.
     *
     * `isSubmitting` поднимаем **до** `launch`, а не внутри: цикл интентов не
     * приостанавливается, пока в канале есть следующий интент, и второй тап по
     * кнопке успел бы увидеть `isSubmitting = false` и отправить второй запрос.
     */
    private fun submit(state: RegisterState) {
        if (!state.canSubmit) return

        updateState {
            copy(
                isSubmitting = true,
                nicknameError = null,
                emailError = null,
                passwordError = null,
                formError = null,
            )
        }
        viewModelScope.launch {
            try {
                register(
                    email = state.email,
                    password = state.password,
                    nickname = state.nickname,
                )
                // Токен уже записан репозиторием — здесь остаётся только навигация.
                updateState { copy(isSubmitting = false) }
                sendEffect(RegisterEffect.NavigateToApp)
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
 * Раскладывает ошибку по полю, которое её вызвало. Незнакомый код сервера поля
 * не назовёт — такую ошибку показываем общим сообщением над кнопкой.
 */
private fun RegisterState.applyError(failure: Throwable): RegisterState {
    val error = failure.toAuthFormError()
    val field = error.targetField()
    return copy(
        isSubmitting = false,
        nicknameError = error.takeIf { field == AuthField.Nickname },
        emailError = error.takeIf { field == AuthField.Email },
        passwordError = error.takeIf { field == AuthField.Password },
        formError = error.takeIf { field == null },
    )
}
