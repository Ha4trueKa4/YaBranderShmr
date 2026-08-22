package com.yandex.avatargenerator.feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

/**
 * Размеры, цвета и типографика онбординга — ровно из макета
 * (Figma `VTr3cYTH7uWGPSqBOjH180`, фреймы 3:261 / 3:281 / 3:322).
 *
 * Здесь остаётся только специфика макета: то, чему есть токен в дизайн-системе,
 * берётся из [AppSpacing] / [AppShape] / [AppTypography] и не дублируется числом.
 *
 * Всё, что несёт цвет, объявлено `@Composable`-свойством — зависит от темы
 * (см. [AppColors]); размеры остаются константами.
 *
 * Макет нарисован под iPhone 393×852; фиксированная высота экрана не переносится —
 * вертикаль распределяется по weight.
 */
internal object OnboardingDefaults {

    // --- общее ---
    val ScreenHorizontalPadding = AppSpacing.l
    val BottomBarPadding = AppSpacing.m

    /** Место под индикатор страниц: страницы резервируют его снизу, чтобы контент не налезал. */
    val BottomContentReserve = AppSpacing.xxxl

    // --- индикатор страниц ---
    val DotSize = 8.dp
    val ActiveDotWidth = 24.dp
    val DotSpacing = AppSpacing.xs

    /** Прозрачность неактивной точки индикатора — цвет ей задаёт вызывающий. */
    const val InactiveDotAlpha = 0.25f

    // --- страница 1: welcome ---
    val WelcomeHorizontalPadding = AppSpacing.xl
    val WelcomeContentSpacing = AppSpacing.xl
    val WelcomeBrandTextSpacing = AppSpacing.s
    val LogoTileSize = 100.dp
    val LogoTileRadius = 28.dp
    val LogoTileBorderWidth = 2.5.dp
    val LogoGlyphSize = 48.dp

    // Плитка лого лежит поверх насыщенного WelcomeGradient — белая в обеих темах.
    val LogoTileFill = Color.White.copy(alpha = 0.13f)
    val LogoTileBorder = Color.White.copy(alpha = 0.25f)

    val WelcomeGradient: Brush
        @Composable @ReadOnlyComposable
        get() = Brush.horizontalGradient(listOf(AppColors.Accent, AppColors.AccentCyan))

    val BrandTitleStyle: TextStyle
        // В макете ExtraBold, но такого начертания Inter в res/font нет — см. AppFont.
        @Composable @ReadOnlyComposable get() = AppTypography.heading1.copy(
            fontWeight = FontWeight.Bold,
            color = AppColors.OnAccent,
        )
    val BrandSubtitleStyle: TextStyle
        @Composable @ReadOnlyComposable
        get() = AppTypography.body.copy(color = AppColors.OnAccent.copy(alpha = 0.8f))

    // --- страница 2: how it works ---
    val PageTitleSpacing = 40.dp
    val StepsSpacing = 28.dp
    val StepRowSpacing = AppSpacing.m
    val StepTextSpacing = AppSpacing.xxs
    val StepIconTileSize = 48.dp
    val StepIconTileRadius = 14.dp
    val StepIconSize = 20.dp

    val StepIconTint: Color
        @Composable @ReadOnlyComposable get() = AppColors.AccentCyan

    val PageTitleStyle: TextStyle
        @Composable @ReadOnlyComposable get() = AppTypography.heading2
    val StepTitleStyle: TextStyle
        @Composable @ReadOnlyComposable get() = AppTypography.body.copy(fontWeight = FontWeight.Bold)

    /** Подпись шага — [AppTypography.label] тем же кеглем, но обычным начертанием. */
    val StepSubtitleStyle: TextStyle
        @Composable @ReadOnlyComposable
        get() = AppTypography.label.copy(fontWeight = FontWeight.Normal)

    // --- страница 3: get started ---
    val GetStartedContentSpacing = AppSpacing.xl
    val IllustrationWidth = 300.dp
    val IllustrationHeight = 320.dp
    val IllustrationRadius = 24.dp
    val IllustrationPadding = AppSpacing.m
    val IllustrationInnerRadius = AppShape.l
    val IllustrationInnerSpacing = AppSpacing.m
    val GlowCircleSize = 100.dp
    val GlowIconSize = 36.dp
    val MockupBrandSpacing = 6.dp

    val CtaGroupSpacing = 20.dp

    /**
     * Cyan в центре → violet по краю. Промежуточные стопы из макета лежат ровно
     * на линейной интерполяции краёв, которую Compose делает сам.
     */
    val GlowBrush: Brush
        @Composable @ReadOnlyComposable
        get() = Brush.radialGradient(listOf(AppColors.AccentCyan, AppColors.Accent))

    val MockupBrandNameStyle: TextStyle
        @Composable @ReadOnlyComposable
        get() = AppTypography.heading3.copy(fontWeight = FontWeight.Bold)

    /** В макете 11sp — берём ближайший токен [AppTypography.caption] (12sp). */
    val MockupTaglineStyle: TextStyle
        @Composable @ReadOnlyComposable get() = AppTypography.caption
    val CtaTitleStyle: TextStyle
        @Composable @ReadOnlyComposable get() = PageTitleStyle
}
