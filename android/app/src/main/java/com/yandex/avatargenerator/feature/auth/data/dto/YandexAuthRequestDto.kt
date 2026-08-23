package com.yandex.avatargenerator.feature.auth.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Тело `POST /auth/yandex` в web-флоу: только `code`. SDK-флоу с `access_token`
 * мобилка не использует — нативный Yandex ID SDK не подключаем.
 */
@Serializable
data class YandexAuthRequestDto(
    @SerialName("code") val code: String,
)
