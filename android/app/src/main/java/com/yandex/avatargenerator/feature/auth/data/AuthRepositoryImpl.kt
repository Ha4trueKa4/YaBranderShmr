package com.yandex.avatargenerator.feature.auth.data

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.AuthTokenStore
import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.core.network.validationError
import com.yandex.avatargenerator.feature.auth.data.dto.AuthResponseDto
import com.yandex.avatargenerator.feature.auth.data.dto.LoginRequestDto
import com.yandex.avatargenerator.feature.auth.data.dto.RegisterRequestDto
import com.yandex.avatargenerator.feature.auth.domain.AuthException
import com.yandex.avatargenerator.feature.auth.domain.AuthRepository
import com.yandex.avatargenerator.feature.auth.domain.model.AuthSession
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val RegisterPath = "auth/register"
private const val LoginPath = "auth/login"
private const val BadRequestCode = 400
private const val ConflictCode = 409

/**
 * Вход и регистрация через API. Токен кладём в [AuthTokenStore] сразу после
 * успешного ответа — дальше его подставляет `HttpClient` сам.
 */
class AuthRepositoryImpl(
    private val client: HttpClient,
    private val tokenStore: AuthTokenStore,
) : AuthRepository {

    override val isAuthorized: Flow<Boolean> = tokenStore.token.map { it != null }

    override suspend fun register(email: String, password: String, nickname: String): AuthSession {
        val response = try {
            apiCall {
                client.post(RegisterPath) {
                    setBody(
                        RegisterRequestDto(
                            email = email.normalizeEmail(),
                            password = password,
                            nickname = nickname.trim(),
                        ),
                    )
                }.body<AuthResponseDto>()
            }
        } catch (clientError: NetworkException.ClientError) {
            throw clientError.toRegistrationFailure()
        }
        return response.toSessionAndRemember()
    }

    override suspend fun login(email: String, password: String): AuthSession {
        val response = try {
            apiCall {
                client.post(LoginPath) {
                    setBody(LoginRequestDto(email = email.normalizeEmail(), password = password))
                }.body<AuthResponseDto>()
            }
        } catch (unauthorized: NetworkException.Unauthorized) {
            // Не «сессия протухла», а неверная пара логин/пароль: токена ещё нет
            throw AuthException.InvalidCredentials(unauthorized)
        }
        return response.toSessionAndRemember()
    }

    override suspend fun logout() {
        tokenStore.clear()
    }

    private suspend fun AuthResponseDto.toSessionAndRemember(): AuthSession {
        tokenStore.update(accessToken)
        return toDomain()
    }
}

/**
 * Бэкенд сравнивает email точно, поэтому `Ivan@mail.ru` и `ivan@mail.ru` для
 * него — разные аккаунты. [Locale.ROOT]: в турецкой локали `I` дало бы `ı`.
 */
private fun String.normalizeEmail(): String = trim().lowercase(Locale.ROOT)

/** Достаёт поле формы из тела `400` и `409`. Прочие 4xx отдаём как есть. */
private suspend fun NetworkException.ClientError.toRegistrationFailure(): Exception {
    if (code != BadRequestCode && code != ConflictCode) return this

    val validationError = validationError()
    val field = when (validationError?.code) {
        "EMAIL_EXISTS", "INVALID_EMAIL" -> AuthException.Field.Email
        "NICKNAME_EXISTS", "INVALID_NICKNAME" -> AuthException.Field.Nickname
        "INVALID_PASSWORD" -> AuthException.Field.Password
        else -> AuthException.Field.Unknown
    }
    val serverMessage = validationError?.message ?: serverMessage

    return if (code == ConflictCode) {
        AuthException.RegistrationConflict(field, serverMessage, this)
    } else {
        AuthException.RegistrationRejected(field, serverMessage, this)
    }
}
