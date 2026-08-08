package com.yandex.avatargenerator.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yandex.avatargenerator.ui.theme.AppColors

/**
 * Корень UI: связывает [NavHostController], нижнюю панель ([AppBottomBar]) и
 * граф ([AppNavHost]).
 *
 * Нижняя панель показывается только на верхнеуровневых вкладках
 * ([TopLevelDestination]); на экранах-деталях (проект, чужой профиль) она
 * скрыта, чтобы не мешать полноэкранному контенту (WebView-редактор и т.п.).
 */
@Composable
fun AvatarGeneratorApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val showBottomBar = currentDestination.isTopLevel() ?: true

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
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
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
    val destination = this ?: return null
    return destination.hierarchy.any { it.route in TopLevelRoutes }
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
