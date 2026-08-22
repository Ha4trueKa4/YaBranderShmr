package com.yandex.avatargenerator.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.auth.ui.LoginScreen
import com.yandex.avatargenerator.feature.auth.ui.RegisterScreen
import com.yandex.avatargenerator.feature.auth.ui.rememberCurrentUser
import com.yandex.avatargenerator.feature.create.ui.ChatsScreen
import com.yandex.avatargenerator.feature.createproject.ui.CreateProjectScreen
import com.yandex.avatargenerator.feature.feed.ui.FeedScreen
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingRoute
import com.yandex.avatargenerator.feature.profile.ui.ProfileScreen
import com.yandex.avatargenerator.feature.project.ui.ProjectScreen
import com.yandex.avatargenerator.feature.project.ui.PublicProjectScreen
import com.yandex.avatargenerator.feature.settings.ui.SettingsScreen
import com.yandex.avatargenerator.feature.subscription.ui.SubscriptionsScreen
import com.yandex.avatargenerator.feature.subscription.ui.SubscriptionsTab
import com.yandex.avatargenerator.ui.components.BrandEmptyState
import com.yandex.avatargenerator.ui.theme.AppColors

/**
 * Граф навигации приложения.
 *
 * Реальными экранами пока подключены онбординг, вход/регистрация, лента, список
 * чатов, профиль и проект; остальные узлы — [PlaceholderScreen] до готовности фич.
 *
 * @param navController контроллер, общий со Scaffold (для нижней панели).
 * @param startDestination стартовый маршрут: лента для авторизованного,
 *  онбординг при первом запуске, иначе вход. Выбирает вызывающий — ему доступны
 *  флаги сессии и онбординга.
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
            // Переход не по нажатию, а по успешному ответу сервера — экран сам
            // сообщит о нём, когда токен уже сохранён.
            LoginScreen(
                onSignedIn = { navController.navigateIntoAppAfterAuth() },
                onNavigateToRegister = {
                    navController.navigate(Destination.Register.route) { launchSingleTop = true }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(Destination.Register.route) {
            RegisterScreen(
                onRegistered = { navController.navigateIntoAppAfterAuth() },
                onNavigateToLogin = {
                    navController.navigate(Destination.Login.route) { launchSingleTop = true }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(Destination.Feed.route) {
            FeedScreen(
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
                    navController.navigate(Destination.Project.createRoute(projectId)) {
                        popUpTo(Destination.CreateProject.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(Destination.Profile.route) {
            val currentUser = rememberCurrentUser()
            if (currentUser != null) {
                ProfileScreen(
                    userId = currentUser.id,
                    isOwnProfile = true,
                    onProjectClick = { projectId ->
                        navController.navigate(Destination.Project.createRoute(projectId))
                    },
                    onActionClick = { /* TODO: экран редактирования профиля */ },
                    onOpenFollowers = {
                        navController.openSubscriptions(currentUser.id, SubscriptionsTab.Followers)
                    },
                    onOpenFollowing = {
                        navController.openSubscriptions(currentUser.id, SubscriptionsTab.Following)
                    },
                    onOpenSettings = {
                        navController.navigate(Destination.Settings.route) { launchSingleTop = true }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ProfileWithoutSessionState(modifier = Modifier.fillMaxSize())
            }
        }

        composable(Destination.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = { navController.navigateToAuthAfterLogout() },
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
                onProjectClick = { projectId ->
                    navController.navigate(Destination.PublicProject.createRoute(projectId))
                },
                onActionClick = { /* Подписка обрабатывается во ViewModel */ },
                onOpenFollowers = {
                    navController.openSubscriptions(userId, SubscriptionsTab.Followers)
                },
                onOpenFollowing = {
                    navController.openSubscriptions(userId, SubscriptionsTab.Following)
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        composable(
            route = Destination.Subscriptions.route,
            arguments = listOf(
                navArgument(Destination.ARG_USER_ID) { type = NavType.StringType },
                navArgument(Destination.ARG_SUBSCRIPTIONS_TAB) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString(Destination.ARG_USER_ID).orEmpty()
            val tab = SubscriptionsTab.fromSlug(
                backStackEntry.arguments?.getString(Destination.ARG_SUBSCRIPTIONS_TAB),
            )
            SubscriptionsScreen(
                userId = userId,
                initialTab = tab,
                onBack = { navController.popBackStack() },
                onUserClick = { clickedUserId ->
                    navController.navigate(Destination.UserProfile.createRoute(clickedUserId))
                },
                currentUserId = rememberCurrentUser()?.id,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Открыть список подписчиков/подписок пользователя [userId] на вкладке [tab]. */
private fun NavHostController.openSubscriptions(userId: String, tab: SubscriptionsTab) {
    navigate(Destination.Subscriptions.createRoute(userId, tab.slug))
}

/**
 * Вкладка профиля, когда id текущего пользователя недоступен: либо он ещё
 * читается из хранилища, либо вход был до появления этого хранилища. Различить
 * нельзя (см. `rememberCurrentUser`), поэтому показываем не индикатор загрузки,
 * а просьбу войти заново — единственное, что тут вообще поможет.
 */
@Composable
private fun ProfileWithoutSessionState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(AppColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        BrandEmptyState(
            title = stringResource(R.string.profile_no_session_title),
            subtitle = stringResource(R.string.profile_no_session_subtitle),
        )
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

/**
 * Разлогин → экран входа. `popUpTo(0) { inclusive = true }` полностью очищает
 * бэкстек (лента, профиль, настройки), чтобы «назад» со входа не возвращал в
 * приложение после выхода из аккаунта.
 */
private fun NavHostController.navigateToAuthAfterLogout() {
    navigate(Destination.Login.route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}
