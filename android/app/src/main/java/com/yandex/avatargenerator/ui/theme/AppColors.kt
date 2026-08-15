package com.yandex.avatargenerator.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Палитра одной темы; в коде читается через [AppColors]. */
@Immutable
internal data class AppColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentCyan: Color,
    val onAccent: Color,
    val onAccentCyan: Color,
    val success: Color,
    val error: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
)

/**
 * Палитра макета.
 *
 * ВАЖНО: при изменении `background` синхронно правь `@color/window_background`
 * в `values-night/colors.xml` — это фон окна до отрисовки Compose.
 */
internal val DarkAppColorScheme = AppColorScheme(
    background = Color(0xFF0F0F14),
    surface = Color(0xFF1A1A24),
    surfaceVariant = Color(0xFF22222F),
    border = Color(0xFF27272A),
    textPrimary = Color(0xFFF4F4F5),
    textSecondary = Color(0xFFA1A1AA),
    textMuted = Color(0xFF52525B),
    accent = Color(0xFF7C3AED),
    accentCyan = Color(0xFF06B6D4),
    onAccent = Color.White,
    onAccentCyan = Color(0xFF0F0F14),
    success = Color(0xFF22C55E),
    error = Color(0xFFEF4444),
    shimmerBase = Color(0xFF27272A),
    shimmerHighlight = Color(0xFF3F3F46),
)

/**
 * Та же шкала (нейтрали zinc, акценты violet/cyan), развёрнутая: акценты взяты
 * на 1–2 ступени темнее, иначе не держат контраст на светлом фоне. Все текстовые
 * токены проходят WCAG AA к `background` (минимум — textMuted, 4.6:1).
 *
 * ВАЖНО: при изменении `background` синхронно правь `@color/window_background`
 * в `values/colors.xml`.
 */
internal val LightAppColorScheme = AppColorScheme(
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF4F4F5),
    border = Color(0xFFE4E4E7),
    textPrimary = Color(0xFF18181B),
    textSecondary = Color(0xFF52525B),
    textMuted = Color(0xFF71717A),
    accent = Color(0xFF6D28D9),
    accentCyan = Color(0xFF0E7490),
    onAccent = Color.White,
    onAccentCyan = Color.White,
    success = Color(0xFF15803D),
    error = Color(0xFFDC2626),
    shimmerBase = Color(0xFFE4E4E7),
    shimmerHighlight = Color(0xFFF4F4F5),
)

internal val LocalAppColorScheme = staticCompositionLocalOf { DarkAppColorScheme }

/**
 * Канонические цветовые токены приложения — единый источник палитры для всех фич.
 *
 * Значения зависят от активной темы, поэтому читаются только внутри
 * `@Composable`-функций. Нужен цвет в обычном коде (`remember { }`, статический
 * `object`) — вычисли его в composable и передай параметром.
 */
internal object AppColors {
    val Background: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.background
    val Surface: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surface
    val SurfaceVariant: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.surfaceVariant
    val Border: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.border

    val TextPrimary: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.textPrimary
    val TextSecondary: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.textSecondary

    /** Приглушённый текст / неактивные элементы (например, вкладки нав-панели). */
    val TextMuted: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.textMuted

    val Accent: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.accent
    val AccentCyan: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.accentCyan

    /** Цвет содержимого поверх заливки Accent (кнопки, беджи). */
    val OnAccent: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onAccent

    /** Цвет содержимого поверх заливки AccentCyan. */
    val OnAccentCyan: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.onAccentCyan

    val Success: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.success
    val Error: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.error

    /** Диагональный градиент Accent → AccentCyan — аватары, превью-плашки проектов. */
    val AccentGradient: Brush
        @Composable @ReadOnlyComposable get() = Brush.linearGradient(listOf(Accent, AccentCyan))

    // Плейсхолдеры скелетона загрузки (shimmer).
    val ShimmerBase: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.shimmerBase
    val ShimmerHighlight: Color
        @Composable @ReadOnlyComposable get() = LocalAppColorScheme.current.shimmerHighlight
}
