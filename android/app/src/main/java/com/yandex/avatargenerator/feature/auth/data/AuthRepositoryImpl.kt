package com.yandex.avatargenerator.feature.auth.data

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.AuthTokenStore
import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.core.network.validationError
import com.yandex.avatargenerator.feature.auth.data.dto.AuthResponseDto
import com.yandex.avatargenerator.feature.auth.data.dto.LoginRequestDto
import com.yandex.avatargenerator.feature.auth.data.dto.RegisterRequestDto
import com.yandex.avatargenerator.feature.auth.data.dto.YandexAuthRequestDto
import com.yandex.avatargenerator.feature.auth.domain.AuthException
import com.yandex.avatargenerator.feature.auth.domain.AuthRepository
import com.yandex.avatargenerator.feature.auth.domain.model.AuthSession
import com.yandex.avatargenerator.feature.auth.domain.model.AuthUser
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val RegisterPath = "auth/register"
private const val LoginPath = "auth/login"
private const val YandexPath = "auth/yandex"
private const val BadRequestCode = 400
private const val ConflictCode = 409

/** Типовой ответ валидации у многих бэкендов; тело такое же, как у `400`. */
private const val UnprocessableEntityCode = 422

/**
 * Единственный тип токена, который умеет подставлять `HttpClient`
 * (`HttpClientFactory` пишет `Authorization: Bearer …` строкой).
 */
private const val BearerTokenType = "bearer"

/**
 * Вход и регистрация через API. Токен кладём в [AuthTokenStore] сразу после
 * успешного ответа — дальше его подставляет `HttpClient` сам, а пользователя из
 * того же ответа — в [AuthUserStore]: ручки `/auth/me` в API нет, и это
 * единственный источник своего id.
 */
class AuthRepositoryImpl(
    private val client: HttpClient,
    private val tokenStore: AuthTokenStore,
    private val userStore: AuthUserStore,
) : AuthRepository {

    override val isAuthorized: Flow<Boolean> = tokenStore.token.map { it != null }

    override val currentUser: Flow<AuthUser?> = userStore.user

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
            throw clientError.toFieldFailure()
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
            throw AuthException.InvalidCredentials(unauthorized)
        } catch (clientError: NetworkException.ClientError) {
            // Формат email сервер отвергает и на входе — показываем ошибку под
            // полем, а не общее «что-то пошло не так».
            throw clientError.toFieldFailure()
        }
        return response.toSessionAndRemember()
    }

    override suspend fun loginWithYandex(code: String): AuthSession {
        val response = try {
            apiCall {
                client.post(YandexPath) {
                    setBody(YandexAuthRequestDto(code = code))
                }.body<AuthResponseDto>()
            }
        } catch (conflict: NetworkException.ClientError) {
            // 409 EMAIL_EXISTS — email занят аккаунтом с паролем. Прочие 4xx
            // (400 INVALID_REQUEST / YANDEX_EMAIL_SCOPE_REQUIRED) — общий сбой
            // провайдера: детали пользователю не показываем.
            throw if (conflict.code == ConflictCode) {
                AuthException.YandexEmailConflict(conflict)
            } else {
                AuthException.YandexAuthFailed(conflict)
            }
        } catch (serverError: NetworkException.ServerError) {
            // 502 YANDEX_OAUTH_FAILED: это не «наш бэкенд лёг», а Yandex ID не
            // отдал профиль — сообщение должно быть про Yandex, а не про сервер.
            throw AuthException.YandexAuthFailed(serverError)
        }
        return response.toSessionAndRemember()
    }

    override suspend fun logout() {
        tokenStore.clear()
        userStore.clear()
    }

    private suspend fun AuthResponseDto.toSessionAndRemember(): AuthSession {
        val session = toDomain()
        // Заголовок собирается строкой "Bearer $token": другой тип токена молча
        // сломал бы все защищённые запросы — лучше упасть здесь и сразу.
        if (!session.tokenType.equals(BearerTokenType, ignoreCase = true)) {
            throw NetworkException.Serialization(
                IllegalStateException(
                    "Ожидали token_type=Bearer, сервер прислал \"${session.tokenType}\"",
                ),
            )
        }
        tokenStore.update(session.accessToken)
        userStore.save(session.user)
        return session
    }
}

/**
 * Бэкенд сравнивает email точно, поэтому `Ivan@mail.ru` и `ivan@mail.ru` для
 * него — разные аккаунты. [Locale.ROOT]: в турецкой локали `I` дало бы `ı`.
 */
private fun String.normalizeEmail(): String = trim().lowercase(Locale.ROOT)

/**
 * Достаёт поле формы из тела `400`, `422` и `409`. Прочие 4xx отдаём как есть.
 * Общий для входа и регистрации: формат email сервер проверяет на обеих ручках.
 */
private suspend fun NetworkException.ClientError.toFieldFailure(): Exception {
    val isRejected = code == BadRequestCode || code == UnprocessableEntityCode
    if (!isRejected && code != ConflictCode) return this

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
        AuthException.FieldRejected(field, serverMessage, this)
    }
}
