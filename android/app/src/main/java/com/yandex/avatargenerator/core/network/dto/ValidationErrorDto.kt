package com.yandex.avatargenerator.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ошибка валидации с привязкой к полю формы: `409` на `POST /auth/register`.
 * Отдельный тип от [ApiErrorDto], потому что там `code` — число, а здесь строка.
 */
@Serializable
data class ValidationErrorDto(
    @SerialName("code") val code: String? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("field") val field: String? = null,
)
