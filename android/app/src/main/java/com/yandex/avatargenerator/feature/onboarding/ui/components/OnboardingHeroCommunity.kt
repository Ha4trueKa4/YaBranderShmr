package com.yandex.avatargenerator.feature.onboarding.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.yandex.avatargenerator.ui.components.BrandAvatar
import com.yandex.avatargenerator.ui.components.BrandBadge
import com.yandex.avatargenerator.ui.components.BrandBadgeVariant
import com.yandex.avatargenerator.ui.components.brandSurface
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/** Иллюстрация четвёртого слайда: лента чужих проектов, автор и экспорт кита. */
@Composable
internal fun OnboardingHeroCommunity(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .brandSurface()
            .padding(AppSpacing.s),
        verticalArrangement = Arrangement.spacedBy(OnboardingDefaults.CommunityCardSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(OnboardingDefaults.CommunityCardSpacing),
            verticalAlignment = Alignment.Top
        ) {
            ProjectMiniCard(
                title = stringResource(R.string.onboarding_mock_project_alpha),
                author = stringResource(R.string.onboarding_mock_project_alpha_author),
                modifier = Modifier.weight(1f)
            )
            // Правая карточка в макете опущена ниже левой — так лента читается живее.
            ProjectMiniCard(
                title = stringResource(R.string.onboarding_mock_project_beta),
                author = stringResource(R.string.onboarding_mock_project_beta_author),
                modifier = Modifier
                    .weight(1f)
                    .padding(top = OnboardingDefaults.CommunityCardOffset)
            )
        }
        CreatorRow()
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            ExportButtonMock(
                text = stringResource(R.string.onboarding_mock_export_png),
                modifier = Modifier.weight(1f)
            )
            ExportButtonMock(
                text = stringResource(R.string.onboarding_mock_export_svg),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ProjectMiniCard(title: String, author: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .brandSurface(
                shape = RoundedCornerShape(AppShape.m),
                color = AppColors.SurfaceVariant
            )
            .padding(AppSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(OnboardingDefaults.CommunityCoverHeight)
                .clip(RoundedCornerShape(AppShape.s))
                .background(AppColors.AccentGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_generate),
                contentDescription = null,
                tint = AppColors.OnAccent
            )
        }
        Text(
            text = title,
            style = AppTypography.label.copy(color = AppColors.TextPrimary)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
        ) {
            BrandAvatar(size = AppSpacing.m)
            Text(text = author, style = AppTypography.caption)
        }
    }
}

@Composable
private fun CreatorRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .brandSurface(
                shape = RoundedCornerShape(AppShape.m),
                color = AppColors.SurfaceVariant
            )
            .padding(AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
    ) {
        BrandAvatar(
            size = OnboardingDefaults.CommunityAvatarSize,
            initials = stringResource(R.string.onboarding_mock_creator_initials)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.onboarding_mock_creator_name),
                style = AppTypography.label.copy(color = AppColors.TextPrimary)
            )
            Text(
                text = stringResource(R.string.onboarding_mock_creator_role),
                style = AppTypography.caption
            )
        }
        BrandBadge(
            text = stringResource(R.string.onboarding_mock_follow),
            variant = BrandBadgeVariant.Accent
        )
    }
}

/** Мини-кнопка экспорта: боевая [com.yandex.avatargenerator.ui.components.BrandTonalButton] в мокапе слишком высокая. */
@Composable
private fun ExportButtonMock(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(OnboardingDefaults.CommunityExportButtonHeight)
            .brandSurface(shape = RoundedCornerShape(AppShape.s)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xxs, Alignment.CenterHorizontally)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_file_text),
            contentDescription = null,
            modifier = Modifier.size(OnboardingDefaults.CommunityExportIconSize),
            tint = AppColors.TextSecondary
        )
        Text(text = text, style = AppTypography.caption)
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingHeroCommunityPreview() {
    AvatarGeneratorTheme {
        Box(
            modifier = Modifier
                .background(AppColors.Background)
                .padding(AppSpacing.m)
        ) {
            OnboardingHeroCommunity(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(OnboardingDefaults.HeroSize)
            )
        }
    }
}
