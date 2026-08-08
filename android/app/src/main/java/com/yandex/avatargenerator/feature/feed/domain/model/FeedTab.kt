package com.yandex.avatargenerator.feature.feed.domain.model

/**
 * Режим ленты.
 *
 * [ALL] — общая лента (все публичные проекты), эндпоинт `GET /feed`.
 * [SUBSCRIPTIONS] — лента подписок, эндпоинт `GET /feed/subscriptions`.
 */
enum class FeedTab {
    ALL,
    SUBSCRIPTIONS,
}
