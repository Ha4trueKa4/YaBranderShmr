package com.yandex.avatargenerator.feature.onboarding.ui.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingDefaults
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Индикатор страниц из макета: активная точка растягивается в «капсулу» 24×8,
 * неактивные остаются кружками 8×8.
 *
 * @param color цвет активной точки. Индикатор лежит поверх всех страниц сразу,
 *   а фон у них разный, поэтому цвет выбирает вызывающий.
 * @param inactiveColor основа для неактивных точек: берётся с прозрачностью
 *   [OnboardingDefaults.InactiveDotAlpha]. По умолчанию — тот же [color];
 *   отдельным задают, когда активная точка акцентная, а остальные нейтральные.
 */
@Composable
internal fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    color: Color,
    modifier: Modifier = Modifier,
    inactiveColor: Color = color,
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
            val dotColor by animateColorAsState(
                targetValue = if (isActive) {
                    color
                } else {
                    inactiveColor.copy(alpha = OnboardingDefaults.InactiveDotAlpha)
                },
                label = "dotColor",
            )
            Spacer(
                modifier = Modifier
                    .size(width = width, height = OnboardingDefaults.DotSize)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}

// Фон берём из палитры, а не хардкодим в @Preview: цвет точек тоже зависит от темы,
// и на прибитом тёмном фоне светлый вариант выглядел бы «тёмным по тёмному».
@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PageIndicatorPreview() {
    AvatarGeneratorTheme {
        Box(modifier = Modifier.background(AppColors.Background).padding(AppSpacing.m)) {
            PageIndicator(
                pageCount = 4,
                currentPage = 1,
                color = AppColors.Accent,
                inactiveColor = AppColors.TextPrimary,
            )
        }
    }
}
