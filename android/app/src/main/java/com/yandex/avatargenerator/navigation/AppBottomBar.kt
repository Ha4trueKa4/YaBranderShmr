package com.yandex.avatargenerator.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy

/**
 * Нижняя панель вкладок.
 *
 * @param currentDestination текущий узел графа (для подсветки активной вкладки).
 * @param onTabSelected клик по вкладке; хост выполняет переход.
 */
@Composable
fun AppBottomBar(
    currentDestination: NavDestination?,
    onTabSelected: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = NavColors.Surface,
        contentColor = NavColors.TextSecondary,
    ) {
        TopLevelDestination.entries.forEach { tab ->
            val selected = currentDestination.isOnTab(tab)
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(tab.labelRes),
                    )
                },
                label = { Text(stringResource(tab.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NavColors.TextPrimary,
                    selectedTextColor = NavColors.TextPrimary,
                    indicatorColor = NavColors.Accent,
                    unselectedIconColor = NavColors.TextSecondary,
                    unselectedTextColor = NavColors.TextSecondary,
                ),
            )
        }
    }
}

/** Активна ли вкладка [tab] с учётом вложенных маршрутов. */
private fun NavDestination?.isOnTab(tab: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.route == tab.destination.route } == true
