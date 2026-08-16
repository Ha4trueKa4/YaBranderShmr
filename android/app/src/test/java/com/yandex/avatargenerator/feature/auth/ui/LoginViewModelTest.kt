package com.yandex.avatargenerator.feature.auth.ui

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.feature.auth.domain.AuthException
import com.yandex.avatargenerator.feature.auth.domain.LoginUseCase
import com.yandex.avatargenerator.feature.auth.fake.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeAuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful login emits SignedIn and passes the entered credentials`() = runTest(dispatcher) {
        val viewModel = LoginViewModel(LoginUseCase(repository))
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("secret123")

        val events = mutableListOf<LoginEvent>()
        val collector = launch { viewModel.events.toList(events) }

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(1, repository.loginCallCount)
        assertEquals("user@example.com", repository.lastEmail)
        assertEquals("secret123", repository.lastPassword)
        assertEquals(listOf(LoginEvent.SignedIn), events)
        assertFalse(viewModel.uiState.value.isSubmitting)

        collector.cancel()
    }

    @Test
    fun `invalid credentials are shown above the button, not under the fields`() = runTest(dispatcher) {
        repository.failure = AuthException.InvalidCredentials()
        val viewModel = LoginViewModel(LoginUseCase(repository))
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("wrong-password")

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthFormError.InvalidCredentials, state.formError)
        // Сервер не сказал, что именно неверно, — подсветка полей была бы враньём.
        assertNull(state.emailError)
        assertNull(state.passwordError)
    }

    @Test
    fun `a rejected email is shown under the email field`() = runTest(dispatcher) {
        repository.failure = AuthException.FieldRejected(AuthException.Field.Email)
        val viewModel = LoginViewModel(LoginUseCase(repository))
        viewModel.onEmailChange("не-email")
        viewModel.onPasswordChange("secret123")

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthFormError.InvalidEmail, state.emailError)
        assertNull(state.formError)
    }

    @Test
    fun `input is capped at the technical limit`() {
        val viewModel = LoginViewModel(LoginUseCase(repository))

        viewModel.onEmailChange("a".repeat(MaxAuthInputLength + 100))
        viewModel.onPasswordChange("b".repeat(MaxAuthInputLength + 100))

        // Это защита от абсурда, а не валидация: правила длины остаются на бэкенде.
        assertEquals(MaxAuthInputLength, viewModel.uiState.value.email.length)
        assertEquals(MaxAuthInputLength, viewModel.uiState.value.password.length)
    }

    @Test
    fun `network failure re-enables the button so the user can retry`() = runTest(dispatcher) {
        repository.failure = NetworkException.NoConnection()
        val viewModel = LoginViewModel(LoginUseCase(repository))
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("secret123")

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthFormError.NoConnection, state.formError)
        assertFalse(state.isSubmitting)
        assertTrue(state.canSubmit)
    }

    @Test
    fun `submit is disabled until both fields are filled`() {
        val viewModel = LoginViewModel(LoginUseCase(repository))

        assertFalse(viewModel.uiState.value.canSubmit)

        // Только email — пароля нет.
        viewModel.onEmailChange("user@example.com")
        assertFalse(viewModel.uiState.value.canSubmit)

        // Только пароль — email нет.
        viewModel.onEmailChange("")
        viewModel.onPasswordChange("secret123")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onEmailChange("user@example.com")
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `submit with an empty form does not reach the repository`() = runTest(dispatcher) {
        val viewModel = LoginViewModel(LoginUseCase(repository))

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(0, repository.loginCallCount)
    }

    @Test
    fun `second submit during a running request does not send another one`() = runTest(dispatcher) {
        repository.suspendUntilResumed = true
        val viewModel = LoginViewModel(LoginUseCase(repository))
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("secret123")

        viewModel.onSubmit()
        advanceUntilIdle()

        // Запрос висит в репозитории: кнопка в состоянии загрузки.
        assertTrue(viewModel.uiState.value.isSubmitting)

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(1, repository.loginCallCount)

        // Отпускаем запрос, чтобы корутина ViewModel не осталась висеть.
        repository.resume()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun `editing a field clears the previous error`() = runTest(dispatcher) {
        repository.failure = AuthException.InvalidCredentials()
        val viewModel = LoginViewModel(LoginUseCase(repository))
        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("wrong-password")

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(AuthFormError.InvalidCredentials, viewModel.uiState.value.formError)

        viewModel.onEmailChange("other@example.com")
        assertNull(viewModel.uiState.value.formError)
    }
}
