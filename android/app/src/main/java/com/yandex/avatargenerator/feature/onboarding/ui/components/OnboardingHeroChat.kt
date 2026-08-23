package com.yandex.avatargenerator.feature.onboarding.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.onboarding.ui.OnboardingDefaults
import com.yandex.avatargenerator.ui.components.BrandBadge
import com.yandex.avatargenerator.ui.components.BrandChatBubble
import com.yandex.avatargenerator.ui.components.brandSurface
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/** Толщина разделительных линий мокапа — точечное значение без токена. */
private val HairlineThickness = 1.dp

/** Доля ширины карточки под входящий блок: столько занимает пузырь в макете. */
private const val IncomingWidthFraction = 0.72f

/** Иллюстрация второго слайда: мокап чата с правкой дизайна и сохранённой версией. */
@Composable
internal fun OnboardingHeroChat(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .brandSurface()
            .padding(OnboardingDefaults.ChatHeaderPadding),
        verticalArrangement = Arrangement.spacedBy(OnboardingDefaults.ChatContentSpacing)
    ) {
        ChatHeader()
        HorizontalDivider(color = AppColors.Border)
        ChatMessages(modifier = Modifier.weight(1f))
        ChatVersionSavedRow()
        ChatInputMock()
    }
}

@Composable
private fun ChatHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.onboarding_mock_chat_project),
            style = AppTypography.label.copy(
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )
        BrandBadge(text = stringResource(R.string.onboarding_mock_chat_private))
    }
}

/** Переписка прижата к низу: при нехватке высоты первым уходит верхний пузырь. */
@Composable
private fun ChatMessages(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds(),
        verticalArrangement = Arrangement.spacedBy(
            OnboardingDefaults.ChatContentSpacing,
            Alignment.Bottom
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            BrandChatBubble(
                text = stringResource(R.string.onboarding_mock_chat_outgoing),
                isOutgoing = true
            )
        }
        IncomingWithPreview(modifier = Modifier.weight(1f))
    }
}

/** Ответ ассистента и мини-превью результата под ним — одним блоком по левому краю. */
@Composable
private fun IncomingWithPreview(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(IncomingWidthFraction),
        verticalArrangement = Arrangement.spacedBy(OnboardingDefaults.ChatContentSpacing)
    ) {
        BrandChatBubble(
            text = stringResource(R.string.onboarding_mock_chat_incoming),
            isOutgoing = false,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = OnboardingDefaults.ChatPreviewHeight)
                .weight(1f)
                .brandSurface(
                    shape = RoundedCornerShape(AppShape.s),
                    color = AppColors.Background
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_generate),
                contentDescription = null,
                modifier = Modifier.size(OnboardingDefaults.ChatPreviewGlyphSize),
                tint = AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ChatVersionSavedRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Hairline(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.onboarding_mock_chat_version_saved),
            style = OnboardingDefaults.MockAccentCaptionStyle,
            modifier = Modifier.padding(horizontal = AppSpacing.xs)
        )
        Hairline(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(HairlineThickness)
            .background(AppColors.Border)
    )
}

@Composable
private fun ChatInputMock(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(OnboardingDefaults.ChatInputHeight)
            .brandSurface(
                shape = RoundedCornerShape(AppShape.full),
                color = AppColors.SurfaceVariant
            )
            .padding(horizontal = AppSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.onboarding_mock_chat_input_hint),
            style = AppTypography.caption,
            modifier = Modifier
                .weight(1f)
                .padding(start = AppSpacing.xs)
        )
        Box(
            modifier = Modifier
                .size(OnboardingDefaults.ChatSendButtonSize)
                .clip(CircleShape)
                .background(AppColors.Accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                modifier = Modifier.size(OnboardingDefaults.ChatSendIconSize),
                tint = AppColors.OnAccent
            )
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingHeroChatPreview() {
    AvatarGeneratorTheme {
        Box(
            modifier = Modifier
                .background(AppColors.Background)
                .padding(AppSpacing.m)
        ) {
            OnboardingHeroChat(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(OnboardingDefaults.HeroSize)
            )
        }
    }
}
