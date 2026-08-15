package com.yandex.avatargenerator.feature.project.domain.model

import java.time.Instant

/**
 * Публичный проект для экрана просмотра по прямой ссылке / из ленты.
 *
 * Отдельная модель от [Project] (`GET /projects/{id}`): здесь собраны данные
 * витрины — автор с состоянием подписки и подборка других его работ, которых в
 * «сыром» проекте нет. Собирается в `data/` (пока [com.yandex.avatargenerator
 * .feature.project.data.MockPublicProjectRepository]); подписи времени и прочий
 * текст форматирует UI, поэтому [publishedAt] хранится как [Instant].
 */
data class PublicProject(
    val id: String,
    val title: String,
    /** Короткая метка поверх hero-баннера (имя/версия брендкита). */
    val badge: String,
    val previewUrl: String?,
    val publishedAt: Instant?,
    val author: PublicProjectAuthor,
    val related: List<PublicProjectRelatedItem>,
)

/** Автор публичного проекта: строка с аватаром, именем, ролью и кнопкой подписки. */
data class PublicProjectAuthor(
    val id: String,
    val name: String,
    val role: String,
    val avatarUrl: String?,
    val isFollowing: Boolean,
)

/** Карточка «More from this creator»: другая работа того же автора. */
data class PublicProjectRelatedItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val previewUrl: String?,
)
