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
 */
enum class TopLevelDestination(
    val destination: Destination,
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int,
) {
    FEED(Destination.Feed, R.drawable.ic_nav_feed, R.string.nav_feed),
    CREATE(Destination.Create, R.drawable.ic_nav_create, R.string.nav_create),
    PROFILE(Destination.Profile, R.drawable.ic_nav_profile, R.string.nav_profile),
}
