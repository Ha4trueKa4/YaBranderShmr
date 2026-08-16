package com.yandex.avatargenerator.core.network

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.dto.ApiErrorDto
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

/**
 * Превращает исключения Ktor в [NetworkException]. Репозитории заворачивают в
 * неё каждый вызов API, чтобы наружу летела только доменная ошибка.
 *
 * ```
 * override suspend fun getProfile(userId: String): UserProfile = apiCall {
 *     client.get("users/$userId").body<UserProfileDto>()
 * }.toDomain()
 * ```
 */
suspend fun <T> apiCall(block: suspend () -> T): T =
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (network: NetworkException) {
        throw network
    } catch (response: ResponseException) {
        throw response.toNetworkException()
    } catch (serialization: SerializationException) {
        throw NetworkException.Serialization(serialization)
    } catch (convert: ContentConvertException) {
        throw NetworkException.Serialization(convert)
    } catch (transformation: NoTransformationFoundException) {
        throw NetworkException.Serialization(transformation)
    } catch (exception: Exception) {
        throw when {
            exception.isTimeout() -> NetworkException.Timeout(exception)
            exception.isConnectionFailure() -> NetworkException.NoConnection(exception)
            else -> NetworkException.Unknown(exception)
        }
    }

private suspend fun ResponseException.toNetworkException(): NetworkException {
    val status = response.status
    val message = readErrorMessage()
    return when {
        status == HttpStatusCode.Unauthorized -> NetworkException.Unauthorized(message, this)
        status == HttpStatusCode.Forbidden -> NetworkException.Forbidden(message, this)
        status == HttpStatusCode.NotFound -> NetworkException.NotFound(message, this)
        status.value in 400..499 -> NetworkException.ClientError(status.value, message, this)
        status.value in 500..599 -> NetworkException.ServerError(status.value, message, this)
        else -> NetworkException.Unknown(this)
    }
}

private suspend fun ResponseException.readErrorMessage(): String? =
    response.decodeErrorBodyOrNull<ApiErrorDto>()?.displayMessage
