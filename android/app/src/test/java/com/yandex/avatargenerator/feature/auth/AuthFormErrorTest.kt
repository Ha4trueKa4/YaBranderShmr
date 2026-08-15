package com.yandex.avatargenerator.feature.auth

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.feature.auth.domain.AuthException
import com.yandex.avatargenerator.feature.auth.ui.AuthField
import com.yandex.avatargenerator.feature.auth.ui.AuthFormError
import com.yandex.avatargenerator.feature.auth.ui.targetField
import com.yandex.avatargenerator.feature.auth.ui.toAuthFormError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Маппинг исключений репозитория в ошибку формы: таблицей, без корутин и Android.
 */
internal class AuthFormErrorTest {

    @Test
    fun `auth exceptions map to their form errors`() {
        val cases: List<Pair<Throwable, AuthFormError>> = listOf(
            AuthException.InvalidCredentials() to AuthFormError.InvalidCredentials,

            AuthException.RegistrationConflict(AuthException.Field.Email) to AuthFormError.EmailTaken,
            AuthException.RegistrationConflict(AuthException.Field.Nickname) to AuthFormError.NicknameTaken,
            // Конфликта по паролю у сервера нет — код нам незнаком.
            AuthException.RegistrationConflict(AuthException.Field.Password) to AuthFormError.Unknown,
            AuthException.RegistrationConflict(AuthException.Field.Unknown) to AuthFormError.Unknown,

            AuthException.RegistrationRejected(AuthException.Field.Email) to AuthFormError.InvalidEmail,
            AuthException.RegistrationRejected(AuthException.Field.Password) to AuthFormError.InvalidPassword,
            AuthException.RegistrationRejected(AuthException.Field.Nickname) to AuthFormError.InvalidNickname,
            AuthException.RegistrationRejected(AuthException.Field.Unknown) to AuthFormError.Unknown,
        )

        cases.forEach { (failure, expected) ->
            assertEquals(failure.toString(), expected, failure.toAuthFormError())
        }
    }

    @Test
    fun `network exceptions map to connection and server errors`() {
        val cases: List<Pair<Throwable, AuthFormError>> = listOf(
            NetworkException.NoConnection() to AuthFormError.NoConnection,
            // Таймаут для пользователя — та же «нет связи».
            NetworkException.Timeout() to AuthFormError.NoConnection,
            NetworkException.ServerError(code = 500) to AuthFormError.ServerUnavailable,
            NetworkException.ServerError(code = 503) to AuthFormError.ServerUnavailable,

            // Остальное классифицировать нечем — общее сообщение.
            NetworkException.Unauthorized() to AuthFormError.Unknown,
            NetworkException.Forbidden() to AuthFormError.Unknown,
            NetworkException.NotFound() to AuthFormError.Unknown,
            NetworkException.ClientError(code = 400) to AuthFormError.Unknown,
            NetworkException.Serialization() to AuthFormError.Unknown,
            NetworkException.Unknown() to AuthFormError.Unknown,
        )

        cases.forEach { (failure, expected) ->
            assertEquals(failure.toString(), expected, failure.toAuthFormError())
        }
    }

    @Test
    fun `an arbitrary exception falls back to Unknown`() {
        assertEquals(AuthFormError.Unknown, RuntimeException("boom").toAuthFormError())
        assertEquals(AuthFormError.Unknown, IllegalStateException().toAuthFormError())
    }

    @Test
    fun `every form error knows its field`() {
        assertEquals(AuthField.Email, AuthFormError.EmailTaken.targetField())
        assertEquals(AuthField.Email, AuthFormError.InvalidEmail.targetField())
        assertEquals(AuthField.Password, AuthFormError.InvalidPassword.targetField())
        assertEquals(AuthField.Nickname, AuthFormError.NicknameTaken.targetField())
        assertEquals(AuthField.Nickname, AuthFormError.InvalidNickname.targetField())

        // Общие ошибки поля не называют — их место над кнопкой.
        assertNull(AuthFormError.InvalidCredentials.targetField())
        assertNull(AuthFormError.NoConnection.targetField())
        assertNull(AuthFormError.ServerUnavailable.targetField())
        assertNull(AuthFormError.Unknown.targetField())
    }
}
