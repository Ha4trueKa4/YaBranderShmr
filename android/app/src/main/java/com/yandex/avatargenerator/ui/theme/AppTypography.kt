package com.yandex.avatargenerator.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Текстовая шкала под конкретную палитру: от темы зависит только цвет в стиле. */
@Immutable
internal data class AppTypographyScheme(
    val heading1: TextStyle,
    val heading2: TextStyle,
    val heading3: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

internal fun appTypographyScheme(colors: AppColorScheme) = AppTypographyScheme(
    heading1 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        color = colors.textPrimary
    ),
    heading2 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        color = colors.textPrimary
    ),
    heading3 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = colors.textPrimary
    ),
    body = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = colors.textPrimary
    ),
    label = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = colors.textSecondary
    ),
    caption = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = colors.textSecondary
    ),
)

internal val LocalAppTypography =
    staticCompositionLocalOf { appTypographyScheme(DarkAppColorScheme) }

/**
 * Именованная текстовая шкала по макету дизайн-системы. Используйте эти стили вместо
 * произвольных font-size — так весь текст в приложении остаётся на одной сетке размеров.
 *
 * `Typography` (Type.kt) собран из этих же стилей и прокидывается в MaterialTheme —
 * так что `MaterialTheme.typography.headlineLarge` и `AppTypography.heading1` — один
 * и тот же стиль под разными путями доступа.
 *
 * Стили несут цвет текста, поэтому читаются только внутри `@Composable`-функций
 * (см. [AppColors]).
 */
object AppTypography {
    val heading1: TextStyle
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current.heading1
    val heading2: TextStyle
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current.heading2
    val heading3: TextStyle
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current.heading3
    val body: TextStyle
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current.body
    val label: TextStyle
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current.label
    val caption: TextStyle
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current.caption
}
