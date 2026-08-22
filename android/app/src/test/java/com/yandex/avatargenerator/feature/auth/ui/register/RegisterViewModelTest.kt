package com.yandex.avatargenerator.feature.auth.ui.register

import com.yandex.avatargenerator.feature.auth.domain.AuthException
import com.yandex.avatargenerator.feature.auth.domain.RegisterUseCase
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
internal class RegisterViewModelTest {

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

    /** Заполняет форму целиком: без этого [RegisterIntent.OnSubmitClick] ничего не делает. */
    private fun RegisterViewModel.fillForm() {
        onIntent(RegisterIntent.OnNicknameChange("Nick"))
        onIntent(RegisterIntent.OnEmailChange("user@example.com"))
        onIntent(RegisterIntent.OnPasswordChange("secret123"))
    }

    @Test
    fun `taken email is shown under the email field`() = runTest(dispatcher) {
        repository.failure = AuthException.RegistrationConflict(AuthException.Field.Email)
        val viewModel = RegisterViewModel(RegisterUseCase(repository))
        viewModel.fillForm()

        viewModel.onIntent(RegisterIntent.OnSubmitClick)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AuthFormError.EmailTaken, state.emailError)
        assertNull(state.formError)
        assertNull(state.passwordError)
        assertNull(state.nicknameError)
    }

    @Test
    fun `rejected password is shown under the password field`() = runTest(dispatcher) {
        repository.failure = AuthException.FieldRejected(AuthException.Field.Password)
        val viewModel = RegisterViewModel(RegisterUseCase(repository))
        viewModel.fillForm()

        viewModel.onIntent(RegisterIntent.OnSubmitClick)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AuthFormError.InvalidPassword, state.passwordError)
        assertNull(state.formError)
        assertNull(state.emailError)
        assertNull(state.nicknameError)
    }

    @Test
    fun `rejected nickname is shown under the nickname field`() = runTest(dispatcher) {
        repository.failure = AuthException.FieldRejected(AuthException.Field.Nickname)
        val viewModel = RegisterViewModel(RegisterUseCase(repository))
        viewModel.fillForm()

        viewModel.onIntent(RegisterIntent.OnSubmitClick)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(AuthFormError.InvalidNickname, state.nicknameError)
        assertNull(state.formError)
        assertNull(state.emailError)
        assertNull(state.passwordError)
    }

    @Test
    fun `unknown server code falls back to the common error above the button`() =
        runTest(dispatcher) {
            repository.failure = AuthException.FieldRejected(AuthException.Field.Unknown)
            val viewModel = RegisterViewModel(RegisterUseCase(repository))
            viewModel.fillForm()

            viewModel.onIntent(RegisterIntent.OnSubmitClick)
            advanceUntilIdle()

            val state = viewModel.state.value
            assertEquals(AuthFormError.Unknown, state.formError)
            // Поле неизвестно — подсвечивать наугад нечего.
            assertNull(state.nicknameError)
            assertNull(state.emailError)
            assertNull(state.passwordError)
        }

    @Test
    fun `input is capped at the technical limit`() = runTest(dispatcher) {
        val viewModel = RegisterViewModel(RegisterUseCase(repository))

        viewModel.onIntent(RegisterIntent.OnNicknameChange("n".repeat(MaxAuthInputLength + 100)))
        viewModel.onIntent(RegisterIntent.OnEmailChange("a".repeat(MaxAuthInputLength + 100)))
        viewModel.onIntent(RegisterIntent.OnPasswordChange("b".repeat(MaxAuthInputLength + 100)))
        advanceUntilIdle()

        // Это защита от абсурда, а не валидация: правила длины остаются на бэкенде.
        val state = viewModel.state.value
        assertEquals(MaxAuthInputLength, state.nickname.length)
        assertEquals(MaxAuthInputLength, state.email.length)
        assertEquals(MaxAuthInputLength, state.password.length)
    }

    @Test
    fun `successful registration emits NavigateToApp and passes all three values`() =
        runTest(dispatcher) {
            val viewModel = RegisterViewModel(RegisterUseCase(repository))
            viewModel.fillForm()

            val effects = mutableListOf<RegisterEffect>()
            val collector = launch { viewModel.effect.toList(effects) }

            viewModel.onIntent(RegisterIntent.OnSubmitClick)
            advanceUntilIdle()

            assertEquals(1, repository.registerCallCount)
            assertEquals("user@example.com", repository.lastEmail)
            assertEquals("secret123", repository.lastPassword)
            assertEquals("Nick", repository.lastNickname)
            assertEquals(listOf(RegisterEffect.NavigateToApp), effects)
            assertFalse(viewModel.state.value.isSubmitting)

            collector.cancel()
        }

    @Test
    fun `submit is disabled until all three fields are filled`() = runTest(dispatcher) {
        val viewModel = RegisterViewModel(RegisterUseCase(repository))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canSubmit)

        viewModel.onIntent(RegisterIntent.OnNicknameChange("Nick"))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.onIntent(RegisterIntent.OnEmailChange("user@example.com"))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.onIntent(RegisterIntent.OnPasswordChange("secret123"))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.canSubmit)
    }

    @Test
    fun `a double tap sends a single request`() = runTest(dispatcher) {
        repository.suspendUntilResumed = true
        val viewModel = RegisterViewModel(RegisterUseCase(repository))
        viewModel.fillForm()

        // Два тапа подряд, без прокрутки диспетчера между ними: именно так
        // выглядит двойное нажатие: флаг isSubmitting обязан подняться до того,
        // как из очереди достанут второй интент.
        viewModel.onIntent(RegisterIntent.OnSubmitClick)
        viewModel.onIntent(RegisterIntent.OnSubmitClick)
        advanceUntilIdle()

        assertEquals(1, repository.registerCallCount)

        repository.resume()
        advanceUntilIdle()
    }

    @Test
    fun `going back to login is a one-shot effect`() = runTest(dispatcher) {
        val viewModel = RegisterViewModel(RegisterUseCase(repository))

        val effects = mutableListOf<RegisterEffect>()
        val collector = launch { viewModel.effect.toList(effects) }

        viewModel.onIntent(RegisterIntent.OnLoginClick)
        advanceUntilIdle()

        assertEquals(listOf(RegisterEffect.NavigateToLogin), effects)
        assertEquals(0, repository.registerCallCount)

        collector.cancel()
    }
}
