package com.yandex.avatargenerator.feature.onboarding.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingDefaults
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/** Первая страница: тёмный фон с фиолетовым свечением, плашка логотипа и название продукта. */
@Composable
internal fun WelcomePage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .background(OnboardingDefaults.WelcomeGlow)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(
                start = OnboardingDefaults.WelcomeHorizontalPadding,
                end = OnboardingDefaults.WelcomeHorizontalPadding,
                bottom = OnboardingDefaults.BottomContentReserve,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = OnboardingDefaults.WelcomeContentSpacing,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        Box(
            modifier = Modifier
                .size(OnboardingDefaults.LogoTileSize)
                .background(
                    color = OnboardingDefaults.LogoTileFill,
                    shape = RoundedCornerShape(OnboardingDefaults.LogoTileRadius),
                )
                .border(
                    width = OnboardingDefaults.LogoTileBorderWidth,
                    color = OnboardingDefaults.LogoTileBorder,
                    shape = RoundedCornerShape(OnboardingDefaults.LogoTileRadius),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_logo_circle_x),
                contentDescription = null,
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(OnboardingDefaults.LogoGlyphSize),
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(OnboardingDefaults.WelcomeBrandTextSpacing),
        ) {
            Text(
                text = stringResource(R.string.onboarding_brand_title),
                style = OnboardingDefaults.BrandTitleStyle,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.onboarding_brand_subtitle),
                style = OnboardingDefaults.BrandSubtitleStyle,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(device = "id:pixel_5", showSystemUi = true)
@Composable
private fun WelcomePagePreview() {
    AvatarGeneratorTheme { WelcomePage() }
}
