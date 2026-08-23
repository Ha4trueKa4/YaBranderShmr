package com.yandex.avatargenerator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

/**
 * Material-схема поверх наших токенов — ради системных примитивов Material3
 * (ripple, `CircularProgressIndicator`, диалоги): свои компоненты берут цвет
 * напрямую из [AppColors].
 */
private fun AppColorScheme.toMaterialColorScheme(darkTheme: Boolean): ColorScheme {
    val base = if (darkTheme) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        secondary = accentSoft,
        onSecondary = onAccentSoft,
        tertiary = accentWarm,
        onTertiary = onAccentWarm,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = textSecondary,
        error = error,
        onError = onAccent,
        outline = border
    )
}

/**
 * Тема приложения: палитра переключается вслед за системной настройкой, фон окна
 * до отрисовки Compose — через `values-night` (см. `res/values/colors.xml`).
 *
 * Dynamic color (Material You) не берём: подмена фирменного акцента цветом обоев
 * ломает узнаваемость бренда.
 */
@Composable
fun AvatarGeneratorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkAppColorScheme else LightAppColorScheme
    val typography = remember(colors) { appTypographyScheme(colors) }

    CompositionLocalProvider(
        LocalAppColorScheme provides colors,
        LocalAppTypography provides typography
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(darkTheme),
            typography = materialTypography(typography),
            content = content
        )
    }
}
