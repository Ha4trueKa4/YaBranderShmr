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

    /**
     * Настройки (тема, язык, аккаунт, разлогин). Открываются из шапки своего
     * профиля; не вкладка, но нижнюю панель показывает (см. `BottomBarDetailRoutes`).
     */
    data object Settings : Destination("settings")

    /**
     * Экран проекта (WebView-редактор + чат). Аргумент пути — id проекта.
     *
     * `job_id` — **необязательный query-аргумент**, а не второй сегмент пути:
     * его знает только форма создания, а на этот же экран ходят ещё из списка
     * чатов и из профиля. Сегмент сделал бы маршрут без задачи невалидным и
     * потребовал бы править все те вызовы; с query они остаются
     * `createRoute(projectId)`.
     */
    data object Project : Destination("project/{$ARG_PROJECT_ID}?$ARG_JOB_ID={$ARG_JOB_ID}") {
        fun createRoute(projectId: String, jobId: String? = null): String =
            "project/${Uri.encode(projectId)}" +
                jobId?.let { "?$ARG_JOB_ID=${Uri.encode(it)}" }.orEmpty()
    }

    /**
     * Экран публичного проекта (просмотр из ленты / по прямой ссылке).
     * Аргумент — id проекта. Отдельно от [Project]: это витрина «только смотреть»
     * (автор, подписка, экспорт), а не редактор своего проекта.
     */
    data object PublicProject : Destination("public-project/{$ARG_PROJECT_ID}") {
        fun createRoute(projectId: String): String = "public-project/${Uri.encode(projectId)}"
    }

    /** Профиль другого пользователя. Аргумент — id пользователя. */
    data object UserProfile : Destination("user/{$ARG_USER_ID}") {
        fun createRoute(userId: String): String = "user/${Uri.encode(userId)}"
    }

    /**
     * Подписчики/подписки пользователя. Открывается с профиля по тапу на счётчик.
     * Аргументы — id пользователя и стартовая вкладка (`followers`/`following`,
     * см. `SubscriptionsTab.slug`).
     */
    data object Subscriptions : Destination("subscriptions/{$ARG_USER_ID}/{$ARG_SUBSCRIPTIONS_TAB}") {
        fun createRoute(userId: String, tabSlug: String): String =
            "subscriptions/${Uri.encode(userId)}/${Uri.encode(tabSlug)}"
    }

    companion object {
        const val ARG_PROJECT_ID = "projectId"
        const val ARG_JOB_ID = "jobId"
        const val ARG_USER_ID = "userId"
        const val ARG_SUBSCRIPTIONS_TAB = "tab"
    }
}
