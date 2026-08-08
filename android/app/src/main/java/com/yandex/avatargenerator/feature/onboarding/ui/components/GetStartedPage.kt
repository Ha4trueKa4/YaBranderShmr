package com.yandex.avatargenerator.feature.onboarding.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingDefaults
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/** Третья страница: мокап логотипа и блок призыва к действию. */
@Composable
internal fun GetStartedPage(
    onGetStartedClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            space = OnboardingDefaults.GetStartedContentSpacing,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        LogoMockup()
        CtaGroup(onGetStartedClick = onGetStartedClick, onSignInClick = onSignInClick)
    }
}

/** Плашка 300×320 с «сгенерированным» логотипом — иллюстрация результата. */
@Composable
private fun LogoMockup(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(OnboardingDefaults.IllustrationWidth)
            .height(OnboardingDefaults.IllustrationHeight)
            .background(AppColors.Surface, RoundedCornerShape(OnboardingDefaults.IllustrationRadius))
            .border(
                width = OnboardingDefaults.BorderWidth,
                color = AppColors.Border,
                shape = RoundedCornerShape(OnboardingDefaults.IllustrationRadius),
            )
            .padding(OnboardingDefaults.IllustrationPadding),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background, RoundedCornerShape(OnboardingDefaults.IllustrationInnerRadius)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = OnboardingDefaults.IllustrationInnerSpacing,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            Box(
                modifier = Modifier
                    .size(OnboardingDefaults.GlowCircleSize)
                    .background(OnboardingDefaults.GlowBrush, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_generate),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(OnboardingDefaults.GlowIconSize),
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(OnboardingDefaults.MockupBrandSpacing),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_mockup_brand_name),
                    style = OnboardingDefaults.MockupBrandNameStyle,
                )
                Text(
                    text = stringResource(R.string.onboarding_mockup_brand_tagline),
                    style = OnboardingDefaults.MockupTaglineStyle,
                )
            }
        }
    }
}

@Composable
private fun CtaGroup(
    onGetStartedClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(OnboardingDefaults.CtaGroupSpacing),
    ) {
        Text(
            text = stringResource(R.string.onboarding_cta_title),
            style = OnboardingDefaults.CtaTitleStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Button(
            onClick = onGetStartedClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(OnboardingDefaults.CtaButtonHeight),
            shape = RoundedCornerShape(OnboardingDefaults.CtaButtonRadius),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                text = stringResource(R.string.onboarding_cta_button),
                style = OnboardingDefaults.CtaButtonLabelStyle,
            )
        }
        SignInPrompt(onSignInClick = onSignInClick)
    }
}

/**
 * «Already have an account? Sign in» — одна строка, кликается только целиком:
 * отдельная кликабельная область на части текста заметно хуже для доступности.
 */
@Composable
private fun SignInPrompt(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val prompt = stringResource(R.string.onboarding_sign_in_prompt)
    val action = stringResource(R.string.onboarding_sign_in_action)
    val text: AnnotatedString = buildAnnotatedString {
        withStyle(SpanStyle(color = OnboardingDefaults.SignInPromptStyle.color)) { append(prompt) }
        append(" ")
        withStyle(
            SpanStyle(
                color = OnboardingDefaults.SignInActionStyle.color,
                fontWeight = OnboardingDefaults.SignInActionStyle.fontWeight,
            ),
        ) { append(action) }
    }
    Text(
        text = text,
        style = OnboardingDefaults.SignInPromptStyle,
        textAlign = TextAlign.Center,
        modifier = modifier.clickable(onClick = onSignInClick),
    )
}

@Preview(device = "id:pixel_5", showSystemUi = true)
@Composable
private fun GetStartedPagePreview() {
    AvatarGeneratorTheme {
        GetStartedPage(onGetStartedClick = {}, onSignInClick = {})
    }
}
