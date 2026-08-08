package com.yandex.avatargenerator.ui.theme

import androidx.compose.material3.Typography

/** MaterialTheme-типографика, собранная из [AppTypography] — единого источника текстовых стилей. */
val Typography = Typography(
    headlineLarge = AppTypography.heading1,
    headlineMedium = AppTypography.heading2,
    headlineSmall = AppTypography.heading3,
    bodyLarge = AppTypography.body,
    labelLarge = AppTypography.label,
    bodySmall = AppTypography.caption
)
