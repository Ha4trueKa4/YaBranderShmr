package com.yandex.avatargenerator.feature.project.domain.model

/**
 * Результат отката (`POST /projects/{id}/versions/{versionId}/restore`).
 *
 * Откат не «переключает» проект на старую версию, а создаёт **новую** поверх
 * истории, поэтому [id] — это id свежесозданной версии, а не той, к которой
 * откатывались. Номера новой версии в ответе нет: за ним нужен перезапрос
 * списка.
 */
data class VersionRestore(
    val id: String,
    /** Ссылка на SVG новой версии. У приватного проекта presigned и протухает. */
    val url: String,
)
