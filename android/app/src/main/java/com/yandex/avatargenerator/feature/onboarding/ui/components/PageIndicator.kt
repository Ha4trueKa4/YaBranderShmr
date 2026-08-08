package com.yandex.avatargenerator.feature.onboarding.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingDefaults
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Индикатор страниц из макета: активная точка растягивается в «капсулу» 24×8,
 * неактивные остаются кружками 8×8.
 */
@Composable
internal fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        R.string.onboarding_page_indicator_description,
        currentPage + 1,
        pageCount,
    )
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(OnboardingDefaults.DotSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val width by animateDpAsState(
                targetValue = if (isActive) OnboardingDefaults.ActiveDotWidth else OnboardingDefaults.DotSize,
                label = "dotWidth",
            )
            val color by animateColorAsState(
                targetValue = if (isActive) OnboardingDefaults.ActiveDotColor else OnboardingDefaults.InactiveDotColor,
                label = "dotColor",
            )
            Spacer(
                modifier = Modifier
                    .size(width = width, height = OnboardingDefaults.DotSize)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun PageIndicatorPreview() {
    AvatarGeneratorTheme {
        PageIndicator(pageCount = 3, currentPage = 1)
    }
}
