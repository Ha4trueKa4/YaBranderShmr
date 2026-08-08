package com.yandex.avatargenerator.feature.onboarding.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingDefaults
import com.yandex.avatargenerator.feature.onboarding.ui.onboardingSteps
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/** Вторая страница: заголовок и список из четырёх шагов. */
@Composable
internal fun HowItWorksPage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(
                start = OnboardingDefaults.ScreenHorizontalPadding,
                end = OnboardingDefaults.ScreenHorizontalPadding,
                bottom = OnboardingDefaults.BottomContentReserve,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = OnboardingDefaults.PageTitleSpacing,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        Text(
            text = stringResource(R.string.onboarding_how_it_works_title),
            style = OnboardingDefaults.PageTitleStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(OnboardingDefaults.StepsSpacing),
        ) {
            onboardingSteps.forEach { step -> StepRow(step = step) }
        }
    }
}

@Preview(device = "id:pixel_5", showSystemUi = true)
@Composable
private fun HowItWorksPagePreview() {
    AvatarGeneratorTheme { HowItWorksPage() }
}
