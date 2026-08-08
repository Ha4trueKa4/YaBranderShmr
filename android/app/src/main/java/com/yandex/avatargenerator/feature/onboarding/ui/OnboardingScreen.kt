package com.yandex.avatargenerator.feature.onboarding.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.yandex.avatargenerator.feature.onboarding.ui.components.GetStartedPage
import com.yandex.avatargenerator.feature.onboarding.ui.components.HowItWorksPage
import com.yandex.avatargenerator.feature.onboarding.ui.components.PageIndicator
import com.yandex.avatargenerator.feature.onboarding.ui.components.WelcomePage
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme
import kotlinx.coroutines.launch

internal const val OnboardingPagerTestTag = "onboarding_pager"

/**
 * Stateless-экран онбординга: три страницы в пейджере и общий индикатор поверх них.
 * Всё состояние приходит снаружи, поэтому экран целиком покрывается превью и тестами.
 */
@Composable
internal fun OnboardingScreen(
    uiState: OnboardingUiState,
    onPageChanged: (Int) -> Unit,
    onGetStartedClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = uiState.currentPage,
        pageCount = { uiState.pages.size },
    )
    val scope = rememberCoroutineScope()

    // Пейджер — источник правды при свайпе; сообщаем позицию во ViewModel.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect(onPageChanged)
    }

    // На первой странице системная «назад» уходит из приложения — BackHandler выключен.
    BackHandler(enabled = pagerState.currentPage > 0) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
    }

    Box(modifier = modifier.fillMaxSize().background(AppColors.Background)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .testTag(OnboardingPagerTestTag),
        ) { page ->
            when (uiState.pages[page]) {
                OnboardingPage.Welcome -> WelcomePage()
                OnboardingPage.HowItWorks -> HowItWorksPage()
                OnboardingPage.GetStarted -> GetStartedPage(
                    onGetStartedClick = onGetStartedClick,
                    onSignInClick = onSignInClick,
                )
            }
        }
        PageIndicator(
            pageCount = uiState.pages.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(bottom = OnboardingDefaults.BottomBarPadding),
        )
    }
}

@Preview(device = "id:pixel_5", showSystemUi = true)
@Composable
private fun OnboardingScreenPreview() {
    AvatarGeneratorTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(),
            onPageChanged = {},
            onGetStartedClick = {},
            onSignInClick = {},
        )
    }
}
