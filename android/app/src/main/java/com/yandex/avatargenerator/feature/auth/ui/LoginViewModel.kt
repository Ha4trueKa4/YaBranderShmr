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
 * ViewModel экрана входа: держит поля формы и по запросу логинит пользователя
 * через [AuthRepository]. Токен репозиторий сохраняет сам — сюда возвращается
 * только результат, по которому шлётся [LoginEvent.SignedIn].
 */
internal class LoginViewModel(
    private val repository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = Channel<LoginEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Правка поля гасит ошибки: старое сообщение относилось к прошлому вводу.
    // Ввод обрезаем по [MaxAuthInputLength] — см. комментарий у константы.
    fun onEmailChange(value: String) = _uiState.update {
        it.copy(email = value.take(MaxAuthInputLength), emailError = null, formError = null)
    }

    fun onPasswordChange(value: String) = _uiState.update {
        it.copy(password = value.take(MaxAuthInputLength), passwordError = null, formError = null)
    }

    fun onPasswordVisibilityToggle() = _uiState.update {
        it.copy(isPasswordVisible = !it.isPasswordVisible)
    }

    /**
     * Логинит и отправляет [LoginEvent.SignedIn].
     *
     * Пустые поля блокируют запуск (кнопка и так неактивна — это страховка на
     * случай гонки с последней рекомпозицией), повторный вызов во время запроса
     * второго запроса не шлёт. Сбой не роняет корутину: снимаем
     * [LoginUiState.isSubmitting] и показываем ошибку, чтобы можно было повторить.
     */
    fun onSubmit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    emailError = null,
                    passwordError = null,
                    formError = null,
                )
            }
            try {
                repository.login(state.email, state.password)
                // Токен уже записан репозиторием — здесь остаётся только навигация.
                _uiState.update { it.copy(isSubmitting = false) }
                _events.send(LoginEvent.SignedIn)
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
 * Раскладывает ошибку по полю, которое её вызвало. Всё, что к полям входа не
 * привязано (в том числе ошибка про никнейм — с этого экрана она прийти не
 * может), показываем общим сообщением над кнопкой.
 */
private fun LoginUiState.applyError(failure: Throwable): LoginUiState {
    val error = failure.toAuthFormError()
    val field = error.targetField()
    return copy(
        isSubmitting = false,
        emailError = error.takeIf { field == AuthField.Email },
        passwordError = error.takeIf { field == AuthField.Password },
        formError = error.takeIf { field != AuthField.Email && field != AuthField.Password },
    )
}
