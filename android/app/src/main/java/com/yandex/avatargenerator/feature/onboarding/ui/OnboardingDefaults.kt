package com.yandex.avatargenerator.feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import kotlin.math.hypot
import kotlin.math.max

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

    // Плитка лого — полупрозрачная подложка цветом текста, поэтому читается в обеих темах.
    val LogoTileFill: Color
        @Composable @ReadOnlyComposable get() = AppColors.TextPrimary.copy(alpha = 0.13f)
    val LogoTileBorder: Color
        @Composable @ReadOnlyComposable get() = AppColors.TextPrimary.copy(alpha = 0.25f)

    /** Центр свечения по вертикали в долях высоты экрана — из макета (840 / 1704). */
    private const val WelcomeGlowCenterY = 0.493f

    /**
     * «Доля радиуса → альфа акцента», снято с макета: прозрачный центр,
     * пик кольца на ~0.37 радиуса, дальше плавное затухание к краям экрана.
     */
    private val WelcomeGlowRing = listOf(
        0f to 0f, 0.06f to 0.02f, 0.13f to 0.10f, 0.19f to 0.24f,
        0.25f to 0.45f, 0.32f to 0.72f, 0.37f to 0.97f, 0.53f to 0.57f,
        0.63f to 0.35f, 0.74f to 0.19f, 0.84f to 0.07f, 1f to 0.02f,
    )

    /**
     * На тёмном фоне акцент светлее базы и работает как свечение; на светлом он
     * темнее базы, и в полную силу кольцо читается грязным — там приглушаем.
     */
    private const val WelcomeGlowLightIntensity = 0.45f

    /**
     * Кольцевое свечение вокруг логотипа (Figma `WI48deZbDftp1OunCXlj5r`, нода 2011:9).
     * Радиус — расстояние до дальнего угла, поэтому свечение тянется на любой экран.
     */
    val WelcomeGlow: Brush
        @Composable get() {
            val accent = AppColors.Accent
            val intensity =
                if (AppColors.Background.luminance() < 0.5f) 1f else WelcomeGlowLightIntensity
            return remember(accent, intensity) {
                object : ShaderBrush() {
                    private val positions = WelcomeGlowRing.map { it.first }
                    private val colors =
                        WelcomeGlowRing.map { accent.copy(alpha = it.second * intensity) }

                    override fun createShader(size: Size): Shader {
                        val center = Offset(size.width / 2f, size.height * WelcomeGlowCenterY)
                        val radius = hypot(
                            max(center.x, size.width - center.x),
                            max(center.y, size.height - center.y),
                        )
                        return RadialGradientShader(center, radius, colors, positions)
                    }
                }
            }
        }

    val BrandTitleStyle: TextStyle
        // В макете ExtraBold, но такого начертания Inter в res/font нет — см. AppFont.
        @Composable @ReadOnlyComposable get() = AppTypography.heading1.copy(
            fontWeight = FontWeight.Bold,
            color = AppColors.TextPrimary,
        )
    val BrandSubtitleStyle: TextStyle
        @Composable @ReadOnlyComposable
        get() = AppTypography.body.copy(color = AppColors.TextSecondary)

    // --- страница 2: how it works ---
    val PageTitleSpacing = 40.dp
    val StepsSpacing = 28.dp
    val StepRowSpacing = AppSpacing.m
    val StepTextSpacing = AppSpacing.xxs
    val StepIconTileSize = 48.dp
    val StepIconTileRadius = 14.dp
    val StepIconSize = 20.dp

    val StepIconTint: Color
        @Composable @ReadOnlyComposable get() = AppColors.AccentSoft

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
     * Мягкий коралл в центре → коралл по краю. Промежуточные стопы из макета лежат ровно
     * на линейной интерполяции краёв, которую Compose делает сам.
     */
    val GlowBrush: Brush
        @Composable @ReadOnlyComposable
        get() = Brush.radialGradient(listOf(AppColors.AccentSoft, AppColors.Accent))

    val MockupBrandNameStyle: TextStyle
        @Composable @ReadOnlyComposable
        get() = AppTypography.heading3.copy(fontWeight = FontWeight.Bold)

    /** В макете 11sp — берём ближайший токен [AppTypography.caption] (12sp). */
    val MockupTaglineStyle: TextStyle
        @Composable @ReadOnlyComposable get() = AppTypography.caption
    val CtaTitleStyle: TextStyle
        @Composable @ReadOnlyComposable get() = PageTitleStyle
}
