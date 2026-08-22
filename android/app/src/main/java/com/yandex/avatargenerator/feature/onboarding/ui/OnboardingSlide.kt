package com.yandex.avatargenerator.feature.onboarding.ui

import androidx.annotation.StringRes
import com.yandex.avatargenerator.R

/**
 * Слайды онбординга в порядке показа. Заголовок в макете двухстрочный: первая
 * строка обычная, вторая — акцентным цветом, поэтому это два разных ресурса.
 */
internal enum class OnboardingSlide(
    @param:StringRes val titleRes: Int,
    @param:StringRes val titleAccentRes: Int,
    @param:StringRes val subtitleRes: Int,
) {
    Generate(
        titleRes = R.string.onboarding_generate_title,
        titleAccentRes = R.string.onboarding_generate_title_accent,
        subtitleRes = R.string.onboarding_generate_subtitle,
    ),
    Chat(
        titleRes = R.string.onboarding_chat_title,
        titleAccentRes = R.string.onboarding_chat_title_accent,
        subtitleRes = R.string.onboarding_chat_subtitle,
    ),
    Community(
        titleRes = R.string.onboarding_community_title,
        titleAccentRes = R.string.onboarding_community_title_accent,
        subtitleRes = R.string.onboarding_community_subtitle,
    ),
}
