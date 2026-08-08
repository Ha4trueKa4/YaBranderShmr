package com.yandex.avatargenerator.feature.feed.domain.model

/** Автор проекта в ленте. Клик по автору ведёт на его профиль. */
data class FeedAuthor(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
)
