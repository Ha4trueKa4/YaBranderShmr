package com.yandex.avatargenerator.feature.project.domain.model

import java.time.Instant

/**
 * Одна версия проекта — элемент истории (`GET /projects/{id}/versions`).
 *
 * Ссылки на сам SVG здесь нет: список отдаёт только метаданные, а `url`
 * приходится запрашивать отдельно на каждую версию
 * ([com.yandex.avatargenerator.feature.project.domain.VersionsRepository.getVersionUrl]).
 * Заведён тикет бэкенду — отдавать `url` прямо в элементах списка.
 *
 * `id` и [number] не-nullable: версия без них — сломанный контракт, а не
 * «пустое» значение, поэтому маппер бросает
 * [com.yandex.avatargenerator.core.domain.NetworkException.Serialization].
 */
data class ProjectVersion(
    val id: String,
    /** Порядковый номер версии (`version_num`), с 1 и по возрастанию. */
    val number: Int,
    val createdAt: Instant?,
)
