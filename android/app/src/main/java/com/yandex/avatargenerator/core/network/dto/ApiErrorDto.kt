package com.yandex.avatargenerator.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Тело ошибки бэкенда (`Error`). Все поля опциональные: разбор ошибки не
 * должен падать сам.
 */
@Serializable
data class ApiErrorDto(
    @SerialName("message") val message: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("detail") val detail: String? = null,
    @SerialName("code") val code: String? = null,
) {

    /** Первое непустое поле, которое можно показать пользователю. */
    val displayMessage: String?
        get() = listOfNotNull(message, detail, error).firstOrNull { it.isNotBlank() }
}
