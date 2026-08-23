package com.yandex.avatargenerator.feature.onboarding.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingDefaults
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/** Иллюстрация первого слайда: акцентная плитка логотипа и название продукта. */
@Composable
internal fun OnboardingHeroGenerate(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(OnboardingDefaults.LogoTileSize)
                .clip(RoundedCornerShape(OnboardingDefaults.LogoTileRadius))
                .background(AppColors.Accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_generate),
                contentDescription = null,
                modifier = Modifier.size(OnboardingDefaults.LogoGlyphSize),
                tint = AppColors.OnAccent
            )
        }
        Spacer(modifier = Modifier.height(OnboardingDefaults.LogoBrandSpacing))
        Text(
            text = stringResource(R.string.onboarding_brand_title),
            style = AppTypography.heading1
        )
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingHeroGeneratePreview() {
    AvatarGeneratorTheme {
        Box(
            modifier = Modifier
                .background(AppColors.Background)
                .padding(AppSpacing.m)
        ) {
            OnboardingHeroGenerate(modifier = Modifier.size(OnboardingDefaults.HeroSize))
        }
    }
}
