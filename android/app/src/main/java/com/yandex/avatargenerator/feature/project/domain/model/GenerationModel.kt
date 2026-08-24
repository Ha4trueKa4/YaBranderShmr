package com.yandex.avatargenerator.feature.project.domain.model

/**
 * Модель генерации, доступная для выбора в чате (`GET /models`).
 *
 * @property id короткое имя модели — то самое, что уходит в теле
 *  `POST /projects/{id}/generate`. Полный URI из ответа (`gpt://<провайдер>/…`)
 *  для генерации не годится: бэкенд клеит префикс сам, — поэтому срезка живёт в
 *  маппере `data/`, а наружу приходит уже готовый к отправке `id`.
 * @property owner владелец модели (`owned_by`) для подзаголовка опции. Не
 *  гарантирован контрактом: `null` — подзаголовка просто не будет.
 */
data class GenerationModel(
    val id: String,
    val owner: String?,
) {
    companion object {

        /** Дефолт бэкенда: см. `generate_project.cpp:31`, `create_project.cpp:20`. */
        const val DefaultId = "deepseek-v4-flash/latest"
    }
}
