package com.yandex.avatargenerator.navigation

import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy

/**
 * Находится ли узел (с учётом иерархии вложенных графов) на одном из [routes].
 *
 * `null`-получатель означает «узел ещё не определён» (первый кадр до готовности
 * бэкстека) и трактуется как `false`; вызывающий при необходимости обрабатывает
 * этот случай отдельно (см. использования).
 */
internal fun NavDestination?.isOnAnyRoute(routes: Set<String>): Boolean =
    this?.hierarchy?.any { it.route in routes } == true

/** Частный случай [isOnAnyRoute] для одного маршрута. */
internal fun NavDestination?.isOnRoute(route: String): Boolean =
    this?.hierarchy?.any { it.route == route } == true
