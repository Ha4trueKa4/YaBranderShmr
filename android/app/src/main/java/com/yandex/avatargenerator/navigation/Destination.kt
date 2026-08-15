package com.yandex.avatargenerator.navigation

import android.net.Uri

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

    /**
     * Онбординг. Стартовый экран при первом запуске; после прохождения в граф
     * не возвращается — не вкладка, поэтому нижней панели на нём нет.
     */
    data object Onboarding : Destination("onboarding")

    /** Вход по email/паролю. Не вкладка — нижней панели нет. */
    data object Login : Destination("login")

    /** Регистрация нового аккаунта. Не вкладка — нижней панели нет. */
    data object Register : Destination("register")

    /** Общая лента + лента подписок (вкладка). Стартовый экран. */
    data object Feed : Destination("feed")

    /** Создание нового проекта (вкладка). Зона команды «создание/редактирование». */
    data object Create : Destination("create")

    /**
     * Форма создания нового проекта (название, описание, запуск генерации).
     * Открывается поверх вкладки «Создать», без нижней панели.
     */
    data object CreateProject : Destination("create/new")

    /** Свой профиль (вкладка). Зона команды «лента/профиль». */
    data object Profile : Destination("profile")

    /** Экран проекта (WebView-редактор + чат). Аргумент — id проекта. */
    data object Project : Destination("project/{$ARG_PROJECT_ID}") {
        // Uri.encode: id может содержать спецсимволы URI (/, ?, #, пробел),
        // иначе маршрут не совпал бы с паттерном. NavType.StringType декодирует.
        fun createRoute(projectId: String): String = "project/${Uri.encode(projectId)}"
    }

    /** Профиль другого пользователя. Аргумент — id пользователя. */
    data object UserProfile : Destination("user/{$ARG_USER_ID}") {
        fun createRoute(userId: String): String = "user/${Uri.encode(userId)}"
    }

    companion object {
        const val ARG_PROJECT_ID = "projectId"
        const val ARG_USER_ID = "userId"
    }
}
