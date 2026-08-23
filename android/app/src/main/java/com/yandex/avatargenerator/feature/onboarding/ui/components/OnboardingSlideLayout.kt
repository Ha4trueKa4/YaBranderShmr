package com.yandex.avatargenerator.feature.onboarding.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Text
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingDefaults
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingSlide
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Одна страница пейджера: иллюстрация сверху, под ней двухстрочный заголовок
 * (вторая строка акцентная) и подзаголовок. Кнопка и индикатор живут снаружи —
 * в макете они не уезжают вместе с контентом.
 */
@Composable
internal fun OnboardingSlideLayout(
    slide: OnboardingSlide,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = OnboardingDefaults.ScreenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Иллюстрация — чистая бутафория: TalkBack не должен зачитывать
        // ни «Follow», ни «Restore», ни имена из мокапов.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            OnboardingHero(
                slide = slide,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = OnboardingDefaults.HeroSize),
            )
        }

        Spacer(modifier = Modifier.height(OnboardingDefaults.HeroTitleSpacing))

        Text(
            text = stringResource(slide.titleRes),
            style = OnboardingDefaults.TitleStyle,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(slide.titleAccentRes),
            style = OnboardingDefaults.TitleAccentStyle,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(OnboardingDefaults.TitleSubtitleSpacing))

        Text(
            text = stringResource(slide.subtitleRes),
            style = OnboardingDefaults.SubtitleStyle,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(OnboardingDefaults.SlideBottomWeight))
    }
}

/** Иллюстрация слайда. Своя у каждого — общего параметризуемого вида у них нет. */
@Composable
private fun OnboardingHero(slide: OnboardingSlide, modifier: Modifier = Modifier) {
    when (slide) {
        OnboardingSlide.Generate -> OnboardingHeroGenerate(modifier)
        OnboardingSlide.Chat -> OnboardingHeroChat(modifier)
        OnboardingSlide.Community -> OnboardingHeroCommunity(modifier)
    }
}

@Preview(name = "Light", device = "id:pixel_5")
@Preview(name = "Dark", device = "id:pixel_5", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingSlideLayoutPreview() {
    AvatarGeneratorTheme {
        Box(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
            OnboardingSlideLayout(slide = OnboardingSlide.Chat)
        }
    }
}
