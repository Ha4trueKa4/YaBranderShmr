package com.yandex.avatargenerator.ui.theme

import androidx.compose.material3.Typography

/**
 * MaterialTheme-типографика из [AppTypography] — единого источника текстовых
 * стилей. Не константа, потому что стили несут цвет и зависят от темы.
 */
internal fun materialTypography(typography: AppTypographyScheme) = Typography(
    headlineLarge = typography.heading1,
    headlineMedium = typography.heading2,
    headlineSmall = typography.heading3,
    bodyLarge = typography.body,
    labelLarge = typography.label,
    bodySmall = typography.caption
)
