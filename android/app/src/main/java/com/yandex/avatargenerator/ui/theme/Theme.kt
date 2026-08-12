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
    // Приложение пока всегда тёмное: весь UI (лента, навигация, онбординг) свёрстан под
    // тёмную палитру AppColors, а окно — под тёмный windowBackground. Поэтому
    // не следуем системной теме и не берём dynamic color, иначе на устройстве
    // в светлой теме Material-цвета «поплыли» бы на тёмных поверхностях.
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
