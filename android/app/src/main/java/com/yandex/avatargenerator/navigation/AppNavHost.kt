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
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingRoute

/**
 * Граф навигации приложения.
 *
 * Реальными экранами пока подключены онбординг и лента; остальные узлы —
 * [PlaceholderScreen] до готовности фич.
 *
 * @param navController контроллер, общий со Scaffold (для нижней панели).
 * @param startDestination стартовый маршрут: онбординг при первом запуске,
 *  иначе лента. Выбирает вызывающий — ему доступен флаг «онбординг пройден».
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Destination.Onboarding.route) {
            OnboardingRoute(
                onFinish = { navController.navigateFromOnboarding() },
                // TODO: экрана входа ещё нет — пока ведём в ленту (команда «авторизация»).
                onSignIn = { navController.navigateFromOnboarding() },
                modifier = Modifier.fillMaxSize(),
            )
        }

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

/**
 * Уход с онбординга в ленту.
 *
 * `inclusive = true` выбрасывает онбординг из бэкстека: кнопка «назад» с ленты
 * должна закрывать приложение, а не возвращать на уже пройденный онбординг.
 */
private fun NavHostController.navigateFromOnboarding() {
    navigate(Destination.Feed.route) {
        popUpTo(Destination.Onboarding.route) { inclusive = true }
        launchSingleTop = true
    }
}
