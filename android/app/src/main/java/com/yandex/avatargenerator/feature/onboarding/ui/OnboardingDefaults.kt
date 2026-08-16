package com.yandex.avatargenerator.feature.onboarding.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * Макет нарисован под iPhone 393×852; фиксированная высота экрана не переносится —
 * вертикаль распределяется по weight.
 */
internal object OnboardingDefaults {

    val ScreenHorizontalPadding = AppSpacing.l
    val BottomBarPadding = AppSpacing.m

    /** Место под индикатор страниц: страницы резервируют его снизу, чтобы контент не налезал. */
    val BottomContentReserve = AppSpacing.xxxl

    val DotSize = 8.dp
    val ActiveDotWidth = 24.dp
    val DotSpacing = AppSpacing.xs
    val ActiveDotColor = AppColors.OnAccent
    val InactiveDotColor = AppColors.OnAccent.copy(alpha = 0.25f)

    val WelcomeHorizontalPadding = AppSpacing.xl
    val WelcomeContentSpacing = AppSpacing.xl
    val WelcomeBrandTextSpacing = AppSpacing.s
    val LogoTileSize = 100.dp
    val LogoTileRadius = 28.dp
    val LogoTileBorderWidth = 2.5.dp
    val LogoGlyphSize = 48.dp
    val LogoTileFill = Color.White.copy(alpha = 0.13f)
    val LogoTileBorder = Color.White.copy(alpha = 0.25f)

    val WelcomeGradient = Brush.horizontalGradient(listOf(AppColors.Accent, AppColors.AccentCyan))

    val BrandTitleStyle = AppTypography.heading1.copy(
        fontWeight = FontWeight.ExtraBold,
        color = AppColors.OnAccent,
    )
    val BrandSubtitleStyle = AppTypography.body.copy(color = AppColors.OnAccent.copy(alpha = 0.8f))

    val PageTitleSpacing = 40.dp
    val StepsSpacing = 28.dp
    val StepRowSpacing = AppSpacing.m
    val StepTextSpacing = AppSpacing.xxs
    val StepIconTileSize = 48.dp
    val StepIconTileRadius = 14.dp
    val StepIconSize = 20.dp
    val StepIconTint = AppColors.AccentCyan
    val BorderWidth = 1.dp

    val PageTitleStyle = AppTypography.heading2
    val StepTitleStyle = AppTypography.body.copy(fontWeight = FontWeight.Bold)

    /** Подпись шага — [AppTypography.label] тем же кеглем, но обычным начертанием. */
    val StepSubtitleStyle = AppTypography.label.copy(fontWeight = FontWeight.Normal)

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

    /** Cyan в центре → purple по краю. Палитра свечения — токен [AppColors.OnboardingGlow]. */
    val GlowBrush = AppColors.OnboardingGlow

    val MockupBrandNameStyle = AppTypography.heading3.copy(fontWeight = FontWeight.Bold)

    /** В макете 11sp — берём ближайший токен [AppTypography.caption] (12sp). */
    val MockupTaglineStyle = AppTypography.caption
    val CtaTitleStyle = PageTitleStyle
}
