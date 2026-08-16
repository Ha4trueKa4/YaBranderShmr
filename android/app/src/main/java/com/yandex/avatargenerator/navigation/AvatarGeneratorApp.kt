package com.yandex.avatargenerator.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yandex.avatargenerator.feature.auth.ui.rememberIsAuthorized
import com.yandex.avatargenerator.feature.onboarding.ui.rememberHasSeenOnboarding
import com.yandex.avatargenerator.ui.theme.AppColors

/**
 * Корень UI: связывает [NavHostController], нижнюю панель ([AppBottomBar]) и
 * граф ([AppNavHost]).
 *
 * Здесь же выбирается стартовый экран: лента для авторизованного, онбординг при
 * первом запуске, иначе вход. Оба флага (сохранённый токен и «онбординг
 * пройден») читаются асинхронно, поэтому до их первых значений граф не строится.
 *
 * Нижняя панель показывается только на верхнеуровневых вкладках
 * ([TopLevelDestination]); на экранах-деталях (проект, чужой профиль) и на
 * онбординге она скрыта, чтобы не мешать полноэкранному контенту.
 *
 * Инсеты системных баров экранам раздаёт `Scaffold`, но с двумя исключениями:
 *  - онбординг рисуется во всё окно и отступает от баров сам;
 *  - экран проекта получает только верхний инсет — низ он «пробивает» сам, чтобы
 *    фон поля ввода уходил под системную навигацию.
 *
 * Если фича заводит собственный `windowInsetsPadding`, её маршрут нужно добавить
 * в соответствующее исключение, иначе отступы удвоятся.
 */
@Composable
fun AvatarGeneratorApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val hasSeenOnboarding = rememberHasSeenOnboarding()
    val isAuthorized = rememberIsAuthorized()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val showBottomBar = currentDestination.isOnAnyRoute(BottomBarDetailRoutes) ||
        (currentDestination.isTopLevel() ?: (isAuthorized == true))
    val isFullBleed = currentDestination?.isOnRoute(Destination.Onboarding.route)
        ?: (isAuthorized == false && hasSeenOnboarding == false)
    val isBottomFullBleed = currentDestination.isOnRoute(Destination.Project.route)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColors.Background,
        contentWindowInsets = when {
            isFullBleed -> WindowInsets(0)
            isBottomFullBleed -> ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top)
            else -> ScaffoldDefaults.contentWindowInsets
        },
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    currentDestination = currentDestination,
                    onTabSelected = { tab -> navController.navigateToTab(tab) },
                )
            }
        },
    ) { innerPadding ->
        val startDestination = rememberStartDestination(hasSeenOnboarding, isAuthorized)

        if (startDestination != null) {
            AppNavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            )
        }
    }
}

/**
 * Стартовый маршрут, зафиксированный по первым прочитанным значениям флагов
 * «есть сессия» и «онбординг пройден». `null` — хотя бы один ещё читается,
 * граф строить рано.
 *
 * Правило: есть токен — сразу лента; нет токена и онбординг не пройден —
 * онбординг; иначе экран входа. Гостя в ленту не пускаем.
 *
 * Пересчитывать маршрут на каждое значение флагов нельзя, хотя соблазнительно:
 * оба меняются прямо во время сессии — флаг онбординга пишет сам онбординг
 * перед уходом на регистрацию, а токен появляется в момент успешного входа.
 * А `NavHost` пересобирает граф на каждое новое значение `startDestination`,
 * и `NavController.setGraph` при смене графа чистит бэкстек и уходит на новый
 * стартовый экран. Из-за этого только что открытая регистрация схлопывалась
 * в ленту — экран успевал мелькнуть и пропадал.
 *
 * `rememberSaveable`, а не `remember`: иначе поворот экрана на регистрации даёт
 * ровно тот же вылет — граф пересоберётся уже с лентой в качестве стартовой.
 */
@Composable
private fun rememberStartDestination(
    hasSeenOnboarding: Boolean?,
    isAuthorized: Boolean?,
): String? {
    var startDestination by rememberSaveable { mutableStateOf<String?>(null) }
    if (startDestination == null && hasSeenOnboarding != null && isAuthorized != null) {
        startDestination = when {
            isAuthorized -> Destination.Feed.route
            !hasSeenOnboarding -> Destination.Onboarding.route
            else -> Destination.Login.route
        }
    }
    return startDestination
}

/** Маршруты верхнеуровневых вкладок; константа — считается один раз, не на каждой рекомпозиции. */
private val TopLevelRoutes: Set<String> =
    TopLevelDestination.entries.mapTo(HashSet()) { it.destination.route }

/** Витрины (не вкладки), на которых нижняя панель всё равно показывается: публичный проект и чужой профиль. */
private val BottomBarDetailRoutes: Set<String> = setOf(
    Destination.PublicProject.route,
    Destination.UserProfile.route,
)

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
