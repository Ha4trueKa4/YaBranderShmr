package com.yandex.avatargenerator.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.avatargenerator.feature.auth.domain.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel экрана регистрации: держит поля формы и создаёт аккаунт через
 * [AuthRepository]. Токен репозиторий сохраняет сам — сюда возвращается только
 * результат, по которому шлётся [RegisterEvent.Registered].
 *
 * Правила длины пароля и никнейма на клиенте не проверяются: их решает сервер,
 * а экран показывает подсказку по коду ошибки.
 */
internal class RegisterViewModel(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegisterEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Правка поля гасит ошибки: старое сообщение относилось к прошлому вводу.
    fun onNicknameChange(value: String) = _uiState.update {
        it.copy(nickname = value, nicknameError = null, formError = null)
    }

    fun onEmailChange(value: String) = _uiState.update {
        it.copy(email = value, emailError = null, formError = null)
    }

    fun onPasswordChange(value: String) = _uiState.update {
        it.copy(password = value, passwordError = null, formError = null)
    }

    fun onPasswordVisibilityToggle() = _uiState.update {
        it.copy(isPasswordVisible = !it.isPasswordVisible)
    }

    /**
     * Регистрирует и отправляет [RegisterEvent.Registered].
     *
     * Пустые поля блокируют запуск (кнопка и так неактивна — это страховка на
     * случай гонки с последней рекомпозицией), повторный вызов во время запроса
     * второго запроса не шлёт. Сбой не роняет корутину: снимаем
     * [RegisterUiState.isSubmitting] и показываем ошибку, чтобы можно было повторить.
     */
    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    nicknameError = null,
                    emailError = null,
                    passwordError = null,
                    formError = null,
                )
            }
            try {
                repository.register(
                    email = state.email,
                    password = state.password,
                    nickname = state.nickname,
                )
                // Токен уже записан репозиторием — здесь остаётся только навигация.
                _uiState.update { it.copy(isSubmitting = false) }
                _events.send(RegisterEvent.Registered)
            } catch (cancellation: CancellationException) {
                // Отмена корутины — не ошибка формы: пробрасываем, иначе экран
                // останется с бесконечным индикатором в кнопке.
                throw cancellation
            } catch (failure: Exception) {
                _uiState.update { it.applyError(failure) }
            }
        }
    }
}

/**
 * Раскладывает ошибку по полю, которое её вызвало. Незнакомый код сервера поля
 * не назовёт — такую ошибку показываем общим сообщением над кнопкой.
 */
private fun RegisterUiState.applyError(failure: Throwable): RegisterUiState {
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
