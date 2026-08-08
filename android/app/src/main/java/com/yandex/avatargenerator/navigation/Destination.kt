package com.yandex.avatargenerator.navigation

/**
 * Все экраны приложения и их маршруты (routes) для Navigation Compose.
 *
 * Маршруты строковые: type-safe навигация (2.8+) потребовала бы плагина
 * kotlinx-serialization — сознательно не тянем его ради лёгкости скелета.
 *
 * Разделение:
 *  - верхнеуровневые ([Feed], [Create], [Profile]) — вкладки нижней панели;
 *  - экраны с аргументами ([Project], [UserProfile]) — открываются поверх,
 *    без нижней панели.
 */
sealed class Destination(val route: String) {

    /** Общая лента + лента подписок (вкладка). Стартовый экран. */
    data object Feed : Destination("feed")

    /** Создание нового проекта (вкладка). Зона команды «создание/редактирование». */
    data object Create : Destination("create")

    /** Свой профиль (вкладка). Зона команды «лента/профиль». */
    data object Profile : Destination("profile")

    /** Экран проекта (WebView-редактор + чат). Аргумент — id проекта. */
    data object Project : Destination("project/{$ARG_PROJECT_ID}") {
        fun createRoute(projectId: String): String = "project/$projectId"
    }

    /** Профиль другого пользователя. Аргумент — id пользователя. */
    data object UserProfile : Destination("user/{$ARG_USER_ID}") {
        fun createRoute(userId: String): String = "user/$userId"
    }

    companion object {
        const val ARG_PROJECT_ID = "projectId"
        const val ARG_USER_ID = "userId"
    }
}
