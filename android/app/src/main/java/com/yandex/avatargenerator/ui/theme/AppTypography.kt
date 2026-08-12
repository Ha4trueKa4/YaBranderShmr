package com.yandex.avatargenerator.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Именованная текстовая шкала по макету дизайн-системы. Используйте эти стили вместо
 * произвольных font-size — так весь текст в приложении остаётся на одной сетке размеров.
 *
 * `Typography` (Type.kt) собран из этих же стилей и прокидывается в MaterialTheme —
 * так что `MaterialTheme.typography.headlineLarge` и `AppTypography.heading1` — один
 * и тот же стиль под разными путями доступа.
 */
object AppTypography {
    val heading1 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        color = AppColors.TextPrimary
    )
    val heading2 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        color = AppColors.TextPrimary
    )
    val heading3 = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = AppColors.TextPrimary
    )
    val body = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = AppColors.TextPrimary
    )
    val label = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = AppColors.TextSecondary
    )
    val caption = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = AppColors.TextSecondary
    )
}
