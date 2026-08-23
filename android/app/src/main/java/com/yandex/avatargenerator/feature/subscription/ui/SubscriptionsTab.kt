package com.yandex.avatargenerator.feature.subscription.ui

/**
 * Вкладка экрана подписок. [slug] — часть маршрута навигации (`.../{tab}`),
 * поэтому строковый и стабильный.
 */
enum class SubscriptionsTab(val slug: String) {
    Followers("followers"),
    Following("following"),
    ;

    companion object {
        /** Разбор аргумента маршрута; неизвестное значение → [Followers]. */
        fun fromSlug(slug: String?): SubscriptionsTab =
            entries.firstOrNull { it.slug == slug } ?: Followers
    }
}
