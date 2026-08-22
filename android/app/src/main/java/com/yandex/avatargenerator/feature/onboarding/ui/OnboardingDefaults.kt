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
import com.yandex.avatargenerator.ui.theme.LocalAppColorScheme
import com.yandex.avatargenerator.ui.theme.LightAppColorScheme

/**
 * Размеры, цвета и типографика онбординга — из макета
 * (Figma `RTTuxBH32XkC0zryIFzuk1`, фреймы 2525:804 / 842 / 891 / 944, все 393×852).
 *
 * Здесь остаётся только специфика макета: то, чему есть токен в дизайн-системе,
 * берётся из [AppSpacing] / [AppShape] / [AppTypography] и не дублируется числом.
 *
 * Всё, что несёт цвет, объявлено `@Composable`-свойством — зависит от темы
 * (см. [AppColors]); размеры остаются константами.
 *
 * Фиксированная высота макета не переносится: вертикаль распределяется по weight,
 * иначе на экранах 4" hero и тексты не помещаются (см. ONBOARDING_REDESIGN_PLAN.md, п. 5.1).
 */
internal object OnboardingDefaults {

    // --- каркас экрана ---
    val ScreenHorizontalPadding = AppSpacing.l
    val BottomBarPadding = AppSpacing.m

    /** Максимум для hero: 285dp по макету; на низких экранах ужимается по weight. */
    val HeroSize = 285.dp

    /** Зазор между hero и первой строкой заголовка (макет: 530 − 436.5). */
    val HeroTitleSpacing = AppSpacing.xxl

    /** Между заголовком и подзаголовком (макет: 615 − 602). */
    val TitleSubtitleSpacing = AppSpacing.m

    val IndicatorButtonSpacing = AppSpacing.l

    /**
     * Доля свободной высоты страницы, уходящая в пустоту под подзаголовком.
     * Остальное забирает иллюстрация, поэтому тексты садятся примерно на 62–72%
     * высоты экрана, как в макете, и при этом не ломаются на низких экранах.
     */
    const val SlideBottomWeight = 0.22f

    /** Соседняя страница пейджера гаснет полностью на ширине одного свайпа. */
    const val MinPageAlpha = 0f

    // --- индикатор страниц ---
    val DotSize = 8.dp
    val ActiveDotWidth = 24.dp
    val DotSpacing = AppSpacing.xs

    /** Прозрачность неактивной точки индикатора — цвет ей задаёт вызывающий. */
    const val InactiveDotAlpha = 0.25f

    // --- слайд 1: плитка лого ---
    val LogoTileSize = 88.dp
    val LogoTileRadius = AppShape.l * 1.5f
    val LogoGlyphSize = 40.dp
    val LogoBrandSpacing = AppSpacing.l

    // --- слайд 2: чат ---
    val ChatHeaderPadding = AppSpacing.s
    val ChatContentSpacing = AppSpacing.xs
    val ChatPreviewHeight = 44.dp
    val ChatPreviewGlyphSize = 20.dp
    val ChatInputHeight = 36.dp
    val ChatSendButtonSize = 28.dp
    val ChatSendIconSize = 14.dp

    // --- слайд 3: версии ---
    val VersionsPreviewHeight = 116.dp
    val VersionsPreviewGlyphSize = 36.dp
    val VersionsSelectionSize = 64.dp
    val VersionsSelectionStroke = 1.dp
    val VersionsSwatchSize = 32.dp

    // --- слайд 4: сообщество ---
    val CommunityCardSpacing = AppSpacing.s
    val CommunityCoverHeight = 64.dp
    val CommunityCardOffset = AppSpacing.m
    val CommunityAvatarSize = 32.dp
    val CommunityExportIconSize = 14.dp

    /** Кнопки экспорта внутри мокапа — визуальная бутафория, ниже боевых 48dp. */
    val CommunityExportButtonHeight = 32.dp

    /**
     * Кольцевое свечение за иллюстрацией: центр — под hero, а не под центром экрана.
     * Центр hero в макете: (151.5 + 285/2) / 852 ≈ 0.345 высоты.
     */
    const val GlowCenterY = 0.345f
    val GlowRadius = 260.dp

    /** На светлой теме то же свечение бьёт по глазам — гасим до едва заметного. */
    private const val GlowLightIntensity = 0.35f

    /**
     * Стопы кольца: в самом центре свечение чуть проседает (за ним лежит hero),
     * пик — на середине радиуса, к краю уходит в прозрачность.
     *
     * Отдаём именно стопы, а не готовый [Brush]: центр и радиус известны только
     * в момент отрисовки, в пикселях (см. `Modifier.onboardingGlow`).
     */
    val GlowStops: Array<Pair<Float, Color>>
        @Composable @ReadOnlyComposable
        get() {
            val accent = AppColors.Accent
            val k = if (LocalAppColorScheme.current === LightAppColorScheme) GlowLightIntensity else 1f
            return arrayOf(
                0.00f to accent.copy(alpha = 0.20f * k),
                0.35f to accent.copy(alpha = 0.34f * k),
                0.65f to accent.copy(alpha = 0.14f * k),
                1.00f to Color.Transparent,
            )
        }

    // --- типографика слайда ---
    val TitleStyle: TextStyle
        @Composable @ReadOnlyComposable
        get() = AppTypography.heading1.copy(color = AppColors.TextPrimary)

    /** Вторая строка заголовка — тот же кегль, акцентный цвет. */
    val TitleAccentStyle: TextStyle
        @Composable @ReadOnlyComposable
        get() = AppTypography.heading1.copy(color = AppColors.Accent)

    val SubtitleStyle: TextStyle
        @Composable @ReadOnlyComposable
        get() = AppTypography.body.copy(color = AppColors.TextSecondary)

    /** Подпись «VERSION 1.2 SAVED» в мокапе чата. */
    val MockAccentCaptionStyle: TextStyle
        @Composable @ReadOnlyComposable
        get() = AppTypography.caption.copy(
            color = AppColors.Accent,
            fontWeight = FontWeight.SemiBold,
        )
}
