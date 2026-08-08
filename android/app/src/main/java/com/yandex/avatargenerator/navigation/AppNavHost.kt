package com.yandex.avatargenerator.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yandex.avatargenerator.feature.feed.ui.FeedScreen

/**
 * Граф навигации приложения.
 *
 * Стартовый экран — [Destination.Feed]. Реальными экранами пока подключён
 * только Feed; остальные узлы — [PlaceholderScreen] до готовности фич.
 *
 * @param navController контроллер, общий со Scaffold (для нижней панели).
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Feed.route,
        modifier = modifier,
    ) {
        composable(Destination.Feed.route) {
            FeedScreen(
                onProjectClick = { projectId ->
                    navController.navigate(Destination.Project.createRoute(projectId))
                },
                onAuthorClick = { authorId ->
                    navController.navigate(Destination.UserProfile.createRoute(authorId))
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(Destination.Create.route) {
            // TODO: экран создания проекта (команда «создание/редактирование»).
            PlaceholderScreen(title = "Создание проекта")
        }

        composable(Destination.Profile.route) {
            // TODO: свой профиль (команда «лента/профиль»).
            PlaceholderScreen(title = "Профиль")
        }

        composable(
            route = Destination.Project.route,
            arguments = listOf(navArgument(Destination.ARG_PROJECT_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Destination.ARG_PROJECT_ID).orEmpty()
            // TODO: экран проекта — WebView-редактор + чат с ИИ.
            PlaceholderScreen(title = "Проект", subtitle = "id: $projectId")
        }

        composable(
            route = Destination.UserProfile.route,
            arguments = listOf(navArgument(Destination.ARG_USER_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString(Destination.ARG_USER_ID).orEmpty()
            // TODO: профиль другого пользователя (команда «лента/профиль»).
            PlaceholderScreen(title = "Профиль пользователя", subtitle = "id: $userId")
        }
    }
}
