package com.yandex.avatargenerator.feature.onboarding.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingDefaults
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingStep
import com.yandex.avatargenerator.feature.onboarding.ui.onboardingSteps
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme
import androidx.compose.ui.semantics.heading

/** Строка списка «How it works»: плитка с иконкой + заголовок и подпись. */
@Composable
internal fun StepRow(
    step: OnboardingStep,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(OnboardingDefaults.StepRowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(OnboardingDefaults.StepIconTileSize)
                .background(AppColors.Surface, RoundedCornerShape(OnboardingDefaults.StepIconTileRadius))
                .border(
                    width = OnboardingDefaults.BorderWidth,
                    color = AppColors.Border,
                    shape = RoundedCornerShape(OnboardingDefaults.StepIconTileRadius),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(step.iconRes),
                contentDescription = null,
                tint = OnboardingDefaults.StepIconTint,
                modifier = Modifier.size(OnboardingDefaults.StepIconSize),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(OnboardingDefaults.StepTextSpacing)) {
            Text(
                text = stringResource(step.titleRes),
                style = OnboardingDefaults.StepTitleStyle,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(step.subtitleRes),
                style = OnboardingDefaults.StepSubtitleStyle,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun StepRowPreview() {
    AvatarGeneratorTheme {
        StepRow(step = onboardingSteps.first())
    }
}
