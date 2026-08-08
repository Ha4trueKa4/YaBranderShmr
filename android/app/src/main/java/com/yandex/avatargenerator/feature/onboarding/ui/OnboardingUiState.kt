package com.yandex.avatargenerator.feature.onboarding.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.yandex.avatargenerator.R

/**
 * Страницы онбординга в порядке показа. Содержимое у страниц разное,
 * поэтому это перечисление, а не список одинаковых data-объектов.
 */
internal enum class OnboardingPage {
    Welcome,
    HowItWorks,
    GetStarted,
}

/** Шаг из списка на странице [OnboardingPage.HowItWorks]. */
internal data class OnboardingStep(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
)

internal val onboardingSteps: List<OnboardingStep> = listOf(
    OnboardingStep(
        iconRes = R.drawable.ic_describe,
        titleRes = R.string.onboarding_step_describe_title,
        subtitleRes = R.string.onboarding_step_describe_subtitle,
    ),
    OnboardingStep(
        iconRes = R.drawable.ic_generate,
        titleRes = R.string.onboarding_step_generate_title,
        subtitleRes = R.string.onboarding_step_generate_subtitle,
    ),
    OnboardingStep(
        iconRes = R.drawable.ic_edit,
        titleRes = R.string.onboarding_step_edit_title,
        subtitleRes = R.string.onboarding_step_edit_subtitle,
    ),
    OnboardingStep(
        iconRes = R.drawable.ic_publish,
        titleRes = R.string.onboarding_step_publish_title,
        subtitleRes = R.string.onboarding_step_publish_subtitle,
    ),
)

internal data class OnboardingUiState(
    val pages: List<OnboardingPage> = OnboardingPage.entries,
    val currentPage: Int = 0,
) {
    val isLastPage: Boolean get() = currentPage == pages.lastIndex
}

/** Одноразовые события для навигации наружу фичи. */
internal sealed interface OnboardingEffect {
    data object NavigateToHome : OnboardingEffect
    data object NavigateToSignIn : OnboardingEffect
}
