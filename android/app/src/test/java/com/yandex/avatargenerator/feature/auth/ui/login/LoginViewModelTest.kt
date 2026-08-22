package com.yandex.avatargenerator.feature.auth.ui.login

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.feature.auth.domain.AuthException
import com.yandex.avatargenerator.feature.auth.domain.LoginUseCase
import com.yandex.avatargenerator.feature.auth.fake.FakeAuthRepository
import com.yandex.avatargenerator.feature.auth.ui.AuthFormError
import com.yandex.avatargenerator.feature.auth.ui.MaxAuthInputLength
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

    /**
     * Интенты обрабатываются в корутине [com.yandex.avatargenerator.core.mvi.BaseViewModel],
     * поэтому после каждой пачки нужно прокрутить диспетчер — иначе state ещё старый.
     */
    private fun LoginViewModel.fillForm(
        email: String = "user@example.com",
        password: String = "secret123",
    ) {
        onIntent(LoginIntent.OnEmailChange(email))
        onIntent(LoginIntent.OnPasswordChange(password))
    }

    @Test
    fun `successful login emits NavigateToApp and passes the entered credentials`() =
        runTest(dispatcher) {
            val viewModel = LoginViewModel(LoginUseCase(repository))
            viewModel.fillForm()

            val effects = mutableListOf<LoginEffect>()
            val collector = launch { viewModel.effect.toList(effects) }

            viewModel.onIntent(LoginIntent.OnSubmitClick)
            advanceUntilIdle()

            assertEquals(1, repository.loginCallCount)
            assertEquals("user@example.com", repository.lastEmail)
            assertEquals("secret123", repository.lastPassword)
            assertEquals(listOf(LoginEffect.NavigateToApp), effects)
            assertFalse(viewModel.state.value.isSubmitting)

            collector.cancel()
        }

    @Test
    fun `invalid credentials are shown above the button, not under the fields`() =
        runTest(dispatcher) {
            repository.failure = AuthException.InvalidCredentials()
            val viewModel = LoginViewModel(LoginUseCase(repository))
            viewModel.fillForm(password = "wrong-password")

            viewModel.onIntent(LoginIntent.OnSubmitClick)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(AuthFormError.InvalidCredentials, state.formError)
            // Сервер не сказал, что именно неверно, — подсветка полей была бы враньём.
            assertNull(state.emailError)
            assertNull(state.passwordError)
        }

    @Test
    fun `a rejected email is shown under the email field`() = runTest(dispatcher) {
        repository.failure = AuthException.FieldRejected(AuthException.Field.Email)
        val viewModel = LoginViewModel(LoginUseCase(repository))
        viewModel.fillForm(email = "не-email")

        viewModel.onIntent(LoginIntent.OnSubmitClick)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AuthFormError.InvalidEmail, state.emailError)
        assertNull(state.formError)
    }

    @Test
    fun `input is capped at the technical limit`() = runTest(dispatcher) {
        val viewModel = LoginViewModel(LoginUseCase(repository))

        viewModel.onIntent(LoginIntent.OnEmailChange("a".repeat(MaxAuthInputLength + 100)))
        viewModel.onIntent(LoginIntent.OnPasswordChange("b".repeat(MaxAuthInputLength + 100)))
        advanceUntilIdle()

        // Это защита от абсурда, а не валидация: правила длины остаются на бэкенде.
        assertEquals(MaxAuthInputLength, viewModel.state.value.email.length)
        assertEquals(MaxAuthInputLength, viewModel.state.value.password.length)
    }

    @Test
    fun `network failure re-enables the button so the user can retry`() = runTest(dispatcher) {
        repository.failure = NetworkException.NoConnection()
        val viewModel = LoginViewModel(LoginUseCase(repository))
        viewModel.fillForm()

        viewModel.onIntent(LoginIntent.OnSubmitClick)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AuthFormError.NoConnection, state.formError)
        assertFalse(state.isSubmitting)
        assertTrue(state.canSubmit)
    }

    @Test
    fun `submit is disabled until both fields are filled`() = runTest(dispatcher) {
        val viewModel = LoginViewModel(LoginUseCase(repository))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canSubmit)

        // Только email — пароля нет.
        viewModel.onIntent(LoginIntent.OnEmailChange("user@example.com"))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.canSubmit)

        // Только пароль — email нет.
        viewModel.onIntent(LoginIntent.OnEmailChange(""))
        viewModel.onIntent(LoginIntent.OnPasswordChange("secret123"))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.onIntent(LoginIntent.OnEmailChange("user@example.com"))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `submit with an empty form does not reach the repository`() = runTest(dispatcher) {
        val viewModel = LoginViewModel(LoginUseCase(repository))

        viewModel.onIntent(LoginIntent.OnSubmitClick)
        advanceUntilIdle()

        assertEquals(0, repository.loginCallCount)
    }

    @Test
    fun `second submit during a running request does not send another one`() = runTest(dispatcher) {
        repository.suspendUntilResumed = true
        val viewModel = LoginViewModel(LoginUseCase(repository))
        viewModel.fillForm()

        viewModel.onIntent(LoginIntent.OnSubmitClick)
        advanceUntilIdle()

        // Запрос висит в репозитории: кнопка в состоянии загрузки.
        assertTrue(viewModel.state.value.isSubmitting)

        viewModel.onIntent(LoginIntent.OnSubmitClick)
        advanceUntilIdle()

        assertEquals(1, repository.loginCallCount)

        // Отпускаем запрос, чтобы корутина ViewModel не осталась висеть.
        repository.resume()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `a double tap sends a single request`() = runTest(dispatcher) {
        repository.suspendUntilResumed = true
        val viewModel = LoginViewModel(LoginUseCase(repository))
        viewModel.fillForm()

        // Два тапа подряд, без прокрутки диспетчера между ними: именно так
        // выглядит двойное нажатие: флаг isSubmitting обязан подняться до того,
        // как из очереди достанут второй интент.
        viewModel.onIntent(LoginIntent.OnSubmitClick)
        viewModel.onIntent(LoginIntent.OnSubmitClick)
        advanceUntilIdle()

        assertEquals(1, repository.loginCallCount)

        repository.resume()
        advanceUntilIdle()
    }

    @Test
    fun `typing is not blocked while the request is in flight`() = runTest(dispatcher) {
        repository.suspendUntilResumed = true
        val viewModel = LoginViewModel(LoginUseCase(repository))
        viewModel.fillForm()

        viewModel.onIntent(LoginIntent.OnSubmitClick)
        advanceUntilIdle()

        // Очередь интентов не должна стоять на времени похода в сеть.
        viewModel.onIntent(LoginIntent.OnEmailChange("other@example.com"))
        advanceUntilIdle()
        assertEquals("other@example.com", viewModel.state.value.email)

        repository.resume()
        advanceUntilIdle()
    }

    @Test
    fun `editing a field clears the previous error`() = runTest(dispatcher) {
        repository.failure = AuthException.InvalidCredentials()
        val viewModel = LoginViewModel(LoginUseCase(repository))
        viewModel.fillForm(password = "wrong-password")

        viewModel.onIntent(LoginIntent.OnSubmitClick)
        advanceUntilIdle()

        assertEquals(AuthFormError.InvalidCredentials, viewModel.state.value.formError)

        viewModel.onIntent(LoginIntent.OnEmailChange("other@example.com"))
        advanceUntilIdle()
        assertNull(viewModel.state.value.formError)
    }

    @Test
    fun `navigation clicks are one-shot effects, not state`() = runTest(dispatcher) {
        val viewModel = LoginViewModel(LoginUseCase(repository))

        val effects = mutableListOf<LoginEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(LoginIntent.OnRegisterClick)
        viewModel.onIntent(LoginIntent.OnForgotPasswordClick)
        advanceUntilIdle()

        assertEquals(
            listOf(LoginEffect.NavigateToRegister, LoginEffect.NavigateToPasswordRecovery),
            effects,
        )
        assertEquals(0, repository.loginCallCount)

        collector.cancel()
    }
}
