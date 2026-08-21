package com.yandex.avatargenerator.feature.project.domain.model

/**
 * Результат генерации по промпту.
 *
 * Полей ровно одно: бэкенд отдаёт только ссылку. Номер новой версии и её
 * `version_id` в ответе не приходят — их подтягивает перезапрос ленты.
 *
 * @property previewUrl presigned-ссылка на SVG; `null` — сервер её не прислал,
 *  но версия при этом создана (ответ 200), так что это не ошибка.
 */
data class GeneratedVersion(val previewUrl: String?)
