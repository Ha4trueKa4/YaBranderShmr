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
    val showBottomBar = currentDestination.isTopLevel()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = NavColors.Background,
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

/** Является ли текущий узел одной из верхнеуровневых вкладок. */
private fun androidx.navigation.NavDestination?.isTopLevel(): Boolean {
    val topLevelRoutes = TopLevelDestination.entries.map { it.destination.route }.toSet()
    return this?.hierarchy?.any { it.route in topLevelRoutes } == true
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
