package com.yandex.avatargenerator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppDarkColorScheme = darkColorScheme(
    primary = AppColors.Accent,
    onPrimary = AppColors.OnAccent,
    secondary = AppColors.AccentCyan,
    onSecondary = AppColors.Background,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceVariant,
    onSurfaceVariant = AppColors.TextSecondary,
    error = AppColors.Error,
    onError = AppColors.OnAccent,
    outline = AppColors.Border
)

@Composable
fun AvatarGeneratorTheme(
    // TODO: вернуть isSystemInDarkTheme()/dynamicColor, когда будет светлая тема.
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography = Typography,
        content = content
    )
}
