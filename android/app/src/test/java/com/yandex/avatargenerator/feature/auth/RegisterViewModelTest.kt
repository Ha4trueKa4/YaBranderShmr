package com.yandex.avatargenerator.feature.auth

import com.yandex.avatargenerator.feature.auth.domain.AuthException
import com.yandex.avatargenerator.feature.auth.ui.AuthFormError
import com.yandex.avatargenerator.feature.auth.ui.MaxAuthInputLength
import com.yandex.avatargenerator.feature.auth.ui.RegisterEvent
import com.yandex.avatargenerator.feature.auth.ui.RegisterViewModel
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

    /** Заполняет форму целиком: без этого [RegisterViewModel.onSubmit] ничего не делает. */
    private fun RegisterViewModel.fillForm() {
        onNicknameChange("Nick")
        onEmailChange("user@example.com")
        onPasswordChange("secret123")
    }

    @Test
    fun `taken email is shown under the email field`() = runTest(dispatcher) {
        repository.failure = AuthException.RegistrationConflict(AuthException.Field.Email)
        val viewModel = RegisterViewModel(repository)
        viewModel.fillForm()

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthFormError.EmailTaken, state.emailError)
        assertNull(state.formError)
        assertNull(state.passwordError)
        assertNull(state.nicknameError)
    }

    @Test
    fun `rejected password is shown under the password field`() = runTest(dispatcher) {
        repository.failure = AuthException.FieldRejected(AuthException.Field.Password)
        val viewModel = RegisterViewModel(repository)
        viewModel.fillForm()

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthFormError.InvalidPassword, state.passwordError)
        assertNull(state.formError)
        assertNull(state.emailError)
        assertNull(state.nicknameError)
    }

    @Test
    fun `rejected nickname is shown under the nickname field`() = runTest(dispatcher) {
        repository.failure = AuthException.FieldRejected(AuthException.Field.Nickname)
        val viewModel = RegisterViewModel(repository)
        viewModel.fillForm()

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthFormError.InvalidNickname, state.nicknameError)
        assertNull(state.formError)
        assertNull(state.emailError)
        assertNull(state.passwordError)
    }

    @Test
    fun `unknown server code falls back to the common error above the button`() = runTest(dispatcher) {
        repository.failure = AuthException.FieldRejected(AuthException.Field.Unknown)
        val viewModel = RegisterViewModel(repository)
        viewModel.fillForm()

        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AuthFormError.Unknown, state.formError)
        // Поле неизвестно — подсвечивать наугад нечего.
        assertNull(state.nicknameError)
        assertNull(state.emailError)
        assertNull(state.passwordError)
    }

    @Test
    fun `input is capped at the technical limit`() {
        val viewModel = RegisterViewModel(repository)

        viewModel.onNicknameChange("n".repeat(MaxAuthInputLength + 100))
        viewModel.onEmailChange("a".repeat(MaxAuthInputLength + 100))
        viewModel.onPasswordChange("b".repeat(MaxAuthInputLength + 100))

        // Это защита от абсурда, а не валидация: правила длины остаются на бэкенде.
        val state = viewModel.uiState.value
        assertEquals(MaxAuthInputLength, state.nickname.length)
        assertEquals(MaxAuthInputLength, state.email.length)
        assertEquals(MaxAuthInputLength, state.password.length)
    }

    @Test
    fun `successful registration emits Registered and passes all three values`() = runTest(dispatcher) {
        val viewModel = RegisterViewModel(repository)
        viewModel.fillForm()

        val events = mutableListOf<RegisterEvent>()
        val collector = launch { viewModel.events.toList(events) }

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(1, repository.registerCallCount)
        assertEquals("user@example.com", repository.lastEmail)
        assertEquals("secret123", repository.lastPassword)
        assertEquals("Nick", repository.lastNickname)
        assertEquals(listOf(RegisterEvent.Registered), events)
        assertFalse(viewModel.uiState.value.isSubmitting)

        collector.cancel()
    }

    @Test
    fun `submit is disabled until all three fields are filled`() {
        val viewModel = RegisterViewModel(repository)

        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onNicknameChange("Nick")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onEmailChange("user@example.com")
        assertFalse(viewModel.uiState.value.canSubmit)

        viewModel.onPasswordChange("secret123")
        assertTrue(viewModel.uiState.value.canSubmit)
    }
}
