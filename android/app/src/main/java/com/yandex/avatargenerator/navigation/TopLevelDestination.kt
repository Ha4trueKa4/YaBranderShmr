package com.yandex.avatargenerator.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.yandex.avatargenerator.R

/**
 * Вкладки нижней навигационной панели.
 *
 * Порядок объявления = порядок вкладок слева направо. Иконки — outline vector
 * drawables (newspaper / plus-circle / user) по макету; перекрашиваются через
 * tint в [AppBottomBar].
 *
 * [detailRoutes] — детальные экраны (не вкладки), открытые из этой вкладки: пока
 * они на экране, панель продолжает подсвечивать вкладку-родителя (публичный проект
 * — «Лента», настройки — «Профиль»).
 */
enum class TopLevelDestination(
    val destination: Destination,
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int,
    val detailRoutes: Set<String> = emptySet(),
) {
    FEED(
        Destination.Feed, R.drawable.ic_nav_feed, R.string.nav_feed,
        detailRoutes = setOf(Destination.PublicProject.route, Destination.UserProfile.route),
    ),
    CREATE(Destination.Create, R.drawable.ic_nav_create, R.string.nav_create),
    PROFILE(
        Destination.Profile, R.drawable.ic_nav_profile, R.string.nav_profile,
        detailRoutes = setOf(Destination.Settings.route),
    ),
}
