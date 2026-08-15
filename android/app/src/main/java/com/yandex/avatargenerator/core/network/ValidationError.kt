package com.yandex.avatargenerator.core.network

import com.yandex.avatargenerator.core.domain.NetworkException
import com.yandex.avatargenerator.core.network.dto.ValidationErrorDto
import io.ktor.client.plugins.ResponseException

/**
 * Разбирает тело ошибки как [ValidationErrorDto] — там, где фиче важен не
 * только статус, но и поле формы. `null`, если тела нет или формат другой.
 */
internal suspend fun NetworkException.validationError(): ValidationErrorDto? {
    val response = (cause as? ResponseException)?.response ?: return null
    return response.decodeErrorBodyOrNull<ValidationErrorDto>()
}
