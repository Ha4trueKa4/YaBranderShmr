package com.yandex.avatargenerator.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.yandex.avatargenerator.R

/**
 * Вкладки нижней навигационной панели.
 *
 * Порядок объявления = порядок вкладок слева направо. Иконки берём из
 * `material-icons-core` (расширенный набор в проект не подключён).
 */
enum class TopLevelDestination(
    val destination: Destination,
    val icon: ImageVector,
    @param:StringRes val labelRes: Int,
) {
    FEED(Destination.Feed, Icons.Filled.Home, R.string.nav_feed),
    CREATE(Destination.Create, Icons.Filled.Add, R.string.nav_create),
    PROFILE(Destination.Profile, Icons.Filled.Person, R.string.nav_profile),
}
