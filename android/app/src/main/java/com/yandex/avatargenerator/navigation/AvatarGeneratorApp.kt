package com.yandex.avatargenerator.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yandex.avatargenerator.feature.onboarding.ui.rememberHasSeenOnboarding
import com.yandex.avatargenerator.ui.theme.AppColors

/**
 * Корень UI: связывает [NavHostController], нижнюю панель ([AppBottomBar]) и
 * граф ([AppNavHost]).
 *
 * Здесь же выбирается стартовый экран: онбординг при первом запуске, иначе
 * лента. Флаг лежит в DataStore и читается асинхронно, поэтому до первого
 * значения граф не строится.
 *
 * Нижняя панель показывается только на верхнеуровневых вкладках
 * ([TopLevelDestination]); на экранах-деталях (проект, чужой профиль) и на
 * онбординге она скрыта, чтобы не мешать полноэкранному контенту.
 */
@Composable
fun AvatarGeneratorApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val hasSeenOnboarding = rememberHasSeenOnboarding()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    // Узел ещё не определён (первый кадр либо флаг не прочитан) — ориентируемся
    // на будущий стартовый экран: верхнеуровневый только если онбординг пройден.
    val showBottomBar = currentDestination.isTopLevel() ?: (hasSeenOnboarding == true)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColors.Background,
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentDestination = currentDestination,
                    onTabSelected = { tab -> navController.navigateToTab(tab) },
                )
            }
        },
    ) { innerPadding ->
        // startDestination задаётся при создании NavHost и потом не меняется,
        // поэтому пока флаг не прочитан (null) — держим пустой фон Scaffold,
        // иначе стартовый экран выберется наугад.
        val startDestination = when (hasSeenOnboarding) {
            null -> null
            true -> Destination.Feed.route
            false -> Destination.Onboarding.route
        }

        if (startDestination != null) {
            AppNavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

/** Маршруты верхнеуровневых вкладок; константа — считается один раз, не на каждой рекомпозиции. */
private val TopLevelRoutes: Set<String> =
    TopLevelDestination.entries.mapTo(HashSet()) { it.destination.route }

/**
 * Является ли текущий узел одной из верхнеуровневых вкладок.
 *
 * Возвращает `null`, когда узел ещё не определён (первый кадр до готовности
 * бэкстека) — вызывающий трактует это как «показать панель» (стартовый экран
 * всегда верхнеуровневый), чтобы панель не мигала на старте.
 */
private fun androidx.navigation.NavDestination?.isTopLevel(): Boolean? {
    if (this == null) return null
    return isOnAnyRoute(TopLevelRoutes)
}

/**
 * Переход на вкладку по стандартному паттерну bottom-навигации:
 *  - popUpTo стартового узла с сохранением состояния (не копим бэкстек вкладок);
 *  - launchSingleTop — без дублей;
 *  - restoreState — восстанавливаем прокрутку/состояние ранее открытой вкладки.
 */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
