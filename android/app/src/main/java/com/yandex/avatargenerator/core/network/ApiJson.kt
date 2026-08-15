package com.yandex.avatargenerator.core.network

import kotlinx.serialization.json.Json

/**
 * Общая настройка сериализации для обмена с API.
 *
 * `isLenient` и `coerceInputValues` намеренно выключены: они молча чинят
 * расхождение контракта вместо честной ошибки разбора.
 */
val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
