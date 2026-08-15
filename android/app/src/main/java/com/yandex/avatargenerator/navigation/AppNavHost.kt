package com.yandex.avatargenerator.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yandex.avatargenerator.feature.auth.ui.LoginScreen
import com.yandex.avatargenerator.feature.auth.ui.RegisterScreen
import com.yandex.avatargenerator.feature.create.ui.ChatsScreen
import com.yandex.avatargenerator.feature.createproject.ui.CreateProjectScreen
import com.yandex.avatargenerator.feature.feed.ui.FeedScreen
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingRoute
import com.yandex.avatargenerator.feature.profile.ui.ProfileScreen
import com.yandex.avatargenerator.feature.project.ui.ProjectScreen
import com.yandex.avatargenerator.feature.project.ui.PublicProjectScreen

/**
 * Граф навигации приложения.
 *
 * Реальными экранами пока подключены онбординг, вход/регистрация, лента, список
 * чатов, профиль и проект; остальные узлы — [PlaceholderScreen] до готовности фич.
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
                onFinish = {
                    navController.navigate(Destination.Register.route) {
                        popUpTo(Destination.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSignIn = {
                    navController.navigate(Destination.Login.route) {
                        popUpTo(Destination.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(Destination.Login.route) {
            LoginScreen(
                onSignInClick = { navController.navigateIntoAppAfterAuth() },
                onNavigateToRegister = {
                    navController.navigate(Destination.Register.route) { launchSingleTop = true }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(Destination.Register.route) {
            RegisterScreen(
                onCreateAccountClick = { navController.navigateIntoAppAfterAuth() },
                onNavigateToLogin = {
                    navController.navigate(Destination.Login.route) { launchSingleTop = true }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(Destination.Feed.route) {
            FeedScreen(
                // Из ленты открываем витрину публичного проекта (только просмотр),
                // а не редактор — редактор доступен из «Создать»/своего профиля.
                onProjectClick = { projectId ->
                    navController.navigate(Destination.PublicProject.createRoute(projectId))
                },
                onAuthorClick = { authorId ->
                    navController.navigate(Destination.UserProfile.createRoute(authorId))
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(Destination.Create.route) {
            ChatsScreen(
                onChatClick = { chatId ->
                    // Чат = проект: id строки — это projectId экрана проекта.
                    navController.navigate(Destination.Project.createRoute(chatId))
                },
                onNewProject = {
                    navController.navigate(Destination.CreateProject.route) { launchSingleTop = true }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(Destination.CreateProject.route) {
            CreateProjectScreen(
                onBack = { navController.popBackStack() },
                onProjectCreated = { projectId ->
                    // Форму создания из бэкстека убираем: «назад» с экрана проекта
                    // возвращает к списку чатов, а не к уже отправленной форме.
                    navController.navigate(Destination.Project.createRoute(projectId)) {
                        popUpTo(Destination.CreateProject.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(Destination.Profile.route) {
            // Свой профиль (вкладка). id текущего пользователя появится из
            // сессии/DI — пока временная заглушка "me".
            ProfileScreen(
                userId = "me",
                isOwnProfile = true,
                onProjectClick = { projectId ->
                    navController.navigate(Destination.Project.createRoute(projectId))
                },
                onActionClick = { /* TODO: экран редактирования профиля */ },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(
            route = Destination.Project.route,
            arguments = listOf(navArgument(Destination.ARG_PROJECT_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Destination.ARG_PROJECT_ID).orEmpty()
            // TODO: WebView-редактор сверху — пока экран проекта это только чат с ИИ.
            ProjectScreen(
                projectId = projectId,
                onBackClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(
            route = Destination.PublicProject.route,
            arguments = listOf(navArgument(Destination.ARG_PROJECT_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString(Destination.ARG_PROJECT_ID).orEmpty()
            PublicProjectScreen(
                projectId = projectId,
                onAuthorClick = { authorId ->
                    navController.navigate(Destination.UserProfile.createRoute(authorId))
                },
                onRelatedClick = { relatedId ->
                    navController.navigate(Destination.PublicProject.createRoute(relatedId))
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(
            route = Destination.UserProfile.route,
            arguments = listOf(navArgument(Destination.ARG_USER_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString(Destination.ARG_USER_ID).orEmpty()
            ProfileScreen(
                userId = userId,
                isOwnProfile = false,
                // Чужой профиль: проекты открываем витриной (только просмотр),
                // как из ленты; редактор — только для своих проектов.
                onProjectClick = { projectId ->
                    navController.navigate(Destination.PublicProject.createRoute(projectId))
                },
                onActionClick = { /* Подписка обрабатывается во ViewModel */ },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Успешный вход/регистрация → лента.
 *
 * `popUpTo(0) { inclusive = true }` полностью очищает бэкстек (онбординг,
 * login/register, их взаимные переходы) — кнопка «назад» с ленты закрывает
 * приложение, а не возвращает на экраны входа.
 */
private fun NavHostController.navigateIntoAppAfterAuth() {
    navigate(Destination.Feed.route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}
