package com.yandex.avatargenerator.feature.onboarding.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.avatargenerator.ui.theme.AppColors

/**
 * Размеры, цвета и типографика онбординга — ровно из макета
 * (Figma `VTr3cYTH7uWGPSqBOjH180`, фреймы 3:261 / 3:281 / 3:322).
 *
 * Держим внутри фичи: общий файл размеров — командная договорённость,
 * заводить его в одиночку не стоит.
 *
 * Макет нарисован под iPhone 393×852 в шрифте Inter. Inter в проект не подключён,
 * поэтому используется системный шрифт с теми же начертаниями; фиксированная высота
 * экрана не переносится — вертикаль распределяется по weight.
 */
internal object OnboardingDefaults {

    // --- общее ---
    val ScreenHorizontalPadding = 24.dp
    val BottomBarPadding = 16.dp

    /** Место под индикатор страниц: страницы резервируют его снизу, чтобы контент не налезал. */
    val BottomContentReserve = 64.dp

    // --- индикатор страниц ---
    val DotSize = 8.dp
    val ActiveDotWidth = 24.dp
    val DotSpacing = 8.dp
    val ActiveDotColor = Color.White
    val InactiveDotColor = Color.White.copy(alpha = 0.251f)

    // --- страница 1: welcome ---
    val WelcomeHorizontalPadding = 32.dp
    val WelcomeContentSpacing = 32.dp
    val WelcomeBrandTextSpacing = 12.dp
    val LogoTileSize = 100.dp
    val LogoTileRadius = 28.dp
    val LogoTileBorderWidth = 2.5.dp
    val LogoGlyphSize = 48.dp
    val LogoTileFill = Color.White.copy(alpha = 0.13f)
    val LogoTileBorder = Color.White.copy(alpha = 0.25f)

    val WelcomeGradient = Brush.horizontalGradient(listOf(AppColors.Accent, AppColors.AccentCyan))

    val BrandTitleStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    val BrandSubtitleStyle =
        TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, color = Color.White.copy(alpha = 0.8f))

    // --- страница 2: how it works ---
    val PageTitleSpacing = 40.dp
    val StepsSpacing = 28.dp
    val StepRowSpacing = 16.dp
    val StepTextSpacing = 4.dp
    val StepIconTileSize = 48.dp
    val StepIconTileRadius = 14.dp
    val StepIconSize = 20.dp
    val StepIconTint = AppColors.AccentCyan
    val BorderWidth = 1.dp

    val PageTitleStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
    val StepTitleStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
    val StepSubtitleStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, color = AppColors.TextSecondary)

    // --- страница 3: get started ---
    val GetStartedContentSpacing = 32.dp
    val IllustrationWidth = 300.dp
    val IllustrationHeight = 320.dp
    val IllustrationRadius = 24.dp
    val IllustrationPadding = 16.dp
    val IllustrationInnerRadius = 16.dp
    val IllustrationInnerSpacing = 16.dp
    val GlowCircleSize = 100.dp
    val GlowIconSize = 36.dp
    val MockupBrandSpacing = 6.dp

    val CtaGroupSpacing = 20.dp
    val CtaButtonHeight = 48.dp
    val CtaButtonRadius = 16.dp

    /** Cyan в центре → purple по краю. Промежуточные стопы взяты из макета. */
    val GlowBrush = Brush.radialGradient(
        0.0f to AppColors.AccentCyan,
        0.125f to Color(0xFF15A7D7),
        0.25f to Color(0xFF2497DA),
        0.5f to Color(0xFF4178E1),
        0.75f to Color(0xFF5F59E7),
        1.0f to AppColors.Accent,
    )

    val MockupBrandNameStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppColors.TextPrimary)
    val MockupTaglineStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal, color = AppColors.TextSecondary)
    val CtaTitleStyle = PageTitleStyle
    val CtaButtonLabelStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.TextPrimary)
    val SignInPromptStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColors.TextSecondary)
    val SignInActionStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.Accent)
}
