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
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    // Узел ещё не определён (первый кадр либо флаг не прочитан) — ориентируемся
    // на будущий стартовый экран: верхнеуровневый только если онбординг пройден.
    val showBottomBar = currentDestination.isTopLevel() ?: (hasSeenOnboarding == true)
    // Тот же приём для полноэкранного режима: до готовности бэкстека онбординг
    // будет открыт ровно тогда, когда флаг ещё не выставлен.
    val isFullBleed = currentDestination?.isOnRoute(Destination.Onboarding.route)
        ?: (hasSeenOnboarding == false)
    // Экран проекта уходит под системную навигацию: фон поля ввода должен
    // доходить до нижнего края экрана (безрамочный низ). Нижний инсет он
    // обрабатывает сам — Scaffold отдаёт ему только верхний.
    val isBottomFullBleed = currentDestination.isOnRoute(Destination.Project.route)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColors.Background,
        // Онбординг занимает всё окно: у него полноэкранный градиент, а отступы
        // от системных баров каждая его страница ставит сама (windowInsetsPadding).
        // Если те же инсеты раздаст ещё и Scaffold — сверху и снизу появится
        // полоса containerColor, а отступы страниц удвоятся.
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
        // Пока флаг не прочитан (null) — держим пустой фон Scaffold, иначе
        // стартовый экран выберется наугад.
        val startDestination = rememberStartDestination(hasSeenOnboarding)

        if (startDestination != null) {
            AppNavHost(
                navController = navController,
                startDestination = startDestination,
                // consumeWindowInsets обязателен вместе с padding (см. KDoc Scaffold):
                // без него вложенный windowInsetsPadding отсчитает системные бары
                // второй раз поверх уже добавленного отступа.
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            )
        }
    }
}

/**
 * Стартовый маршрут, зафиксированный по первому прочитанному значению флага
 * «онбординг пройден». `null` — флаг ещё читается, граф строить рано.
 *
 * Пересчитывать маршрут на каждое значение флага нельзя, хотя соблазнительно:
 * флаг меняется прямо во время сессии — его пишет сам онбординг перед тем, как
 * уйти на регистрацию. А `NavHost` пересобирает граф на каждое новое значение
 * `startDestination`, и `NavController.setGraph` при смене графа чистит бэкстек
 * и уходит на новый стартовый экран. Из-за этого только что открытая регистрация
 * схлопывалась в ленту — экран успевал мелькнуть и пропадал.
 *
 * `rememberSaveable`, а не `remember`: иначе поворот экрана на регистрации даёт
 * ровно тот же вылет — граф пересоберётся уже с лентой в качестве стартовой.
 */
@Composable
private fun rememberStartDestination(hasSeenOnboarding: Boolean?): String? {
    var startDestination by rememberSaveable { mutableStateOf<String?>(null) }
    if (startDestination == null && hasSeenOnboarding != null) {
        startDestination =
            if (hasSeenOnboarding) Destination.Feed.route else Destination.Onboarding.route
    }
    return startDestination
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
