package com.yandex.avatargenerator.feature.onboarding.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.onboarding.ui.components.OnboardingSlideLayout
import com.yandex.avatargenerator.feature.onboarding.ui.components.PageIndicator
import com.yandex.avatargenerator.ui.components.BrandPrimaryButton
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme
import kotlin.math.abs

internal const val OnboardingPagerTestTag = "onboarding_pager"

/**
 * Stateless-каркас онбординга: свайпается только пара «иллюстрация + тексты»,
 * индикатор и кнопка стоят на месте — так же, как в макете.
 */
@Composable
internal fun OnboardingContent(
    state: OnboardingState,
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = state.currentPage,
        pageCount = { state.slides.size },
    )

    // Пейджер и состояние синхронизируются в обе стороны: свайп доезжает до
    // ViewModel, кнопка «Next» — обратно до пейджера. Цикла не возникает, потому
    // что каждая сторона сначала сравнивает значение с текущим.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page -> onIntent(OnboardingIntent.OnPageChange(page)) }
    }
    LaunchedEffect(state.currentPage) {
        if (pagerState.currentPage != state.currentPage) {
            pagerState.animateScrollToPage(state.currentPage)
        }
    }

    BackHandler(enabled = state.currentPage > 0) {
        onIntent(OnboardingIntent.OnBackClick)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .onboardingGlow(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .testTag(OnboardingPagerTestTag),
            ) { page ->
                OnboardingSlideLayout(
                    slide = state.slides[page],
                    modifier = Modifier.graphicsLayer { alpha = pagerState.pageAlpha(page) },
                )
            }

            PageIndicator(
                pageCount = state.slides.size,
                currentPage = pagerState.currentPage,
                color = AppColors.Accent,
                inactiveColor = AppColors.TextPrimary,
            )

            Spacer(modifier = Modifier.height(OnboardingDefaults.IndicatorButtonSpacing))

            BrandPrimaryButton(
                text = stringResource(
                    if (state.isLastPage) R.string.onboarding_get_started else R.string.onboarding_next
                ),
                onClick = { onIntent(OnboardingIntent.OnPrimaryClick) },
                fullWidth = true,
                modifier = Modifier.padding(horizontal = OnboardingDefaults.ScreenHorizontalPadding),
            )

            Spacer(modifier = Modifier.height(OnboardingDefaults.BottomBarPadding))
        }
    }
}

/**
 * Радиальное свечение за иллюстрацией. Центр и радиус известны только в
 * пикселях на отрисовке, поэтому не `Modifier.background(brush)`, а `drawBehind`:
 * он рисует поверх залитого фона, но под содержимым.
 */
@Composable
private fun Modifier.onboardingGlow(): Modifier {
    val stops = OnboardingDefaults.GlowStops
    val radius = OnboardingDefaults.GlowRadius
    return drawBehind {
        drawRect(
            brush = Brush.radialGradient(
                *stops,
                center = Offset(size.width / 2f, size.height * OnboardingDefaults.GlowCenterY),
                radius = radius.toPx(),
            )
        )
    }
}

/**
 * Страница гаснет по мере ухода за край экрана: соседняя видна ровно настолько,
 * насколько её вытянули пальцем.
 */
private fun PagerState.pageAlpha(page: Int): Float {
    val offset = (currentPage - page) + currentPageOffsetFraction
    return (1f - abs(offset)).coerceIn(OnboardingDefaults.MinPageAlpha, 1f)
}

@Preview(name = "Light", device = "id:pixel_5", showSystemUi = true)
@Preview(name = "Dark", device = "id:pixel_5", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Small", device = "id:pixel_2", showSystemUi = true)
@Composable
private fun OnboardingContentPreview() {
    AvatarGeneratorTheme {
        OnboardingContent(state = OnboardingState(), onIntent = {})
    }
}

@Preview(name = "Last slide", device = "id:pixel_5", showSystemUi = true)
@Composable
private fun OnboardingContentLastSlidePreview() {
    AvatarGeneratorTheme {
        OnboardingContent(
            state = OnboardingState(currentPage = OnboardingSlide.entries.lastIndex),
            onIntent = {},
        )
    }
}
