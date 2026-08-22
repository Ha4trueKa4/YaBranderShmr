package com.yandex.avatargenerator.feature.subscription.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.subscription.domain.model.SubscriptionUser
import com.yandex.avatargenerator.ui.components.BrandAvatar
import com.yandex.avatargenerator.ui.components.avatarInitials
import com.yandex.avatargenerator.ui.components.BrandPrimaryButton
import com.yandex.avatargenerator.ui.components.BrandSecondaryButton
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

/**
 * Строка-карточка списка подписок по макету: аватар, имя, число подписчиков и
 * кнопка Follow/Unfollow. Тап по карточке открывает профиль пользователя, тап по
 * кнопке — подписка/отписка. Из примитивов дизайн-системы.
 *
 * @param isSelf это сам текущий пользователь — на себя не подписываются, вместо
 *   кнопки показываем метку «Вы».
 */
@Composable
internal fun SubscriptionUserRow(
    user: SubscriptionUser,
    isSelf: Boolean,
    onClick: () -> Unit,
    onToggleFollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppShape.m))
            .background(AppColors.Surface)
            .clickable(onClick = onClick)
            .padding(AppSpacing.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        BrandAvatar(size = 56.dp, imageUrl = user.avatarUrl, initials = avatarInitials(user.displayName))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
        ) {
            Text(
                text = user.displayName,
                style = AppTypography.body.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Счётчик рисуем только если бэкенд его прислал: контракт списка
            // его не отдаёт, «0 followers» у всех было бы враньём.
            if (user.followersCount != null) {
                Text(
                    text = stringResource(R.string.subscriptions_followers_count, user.followersCount),
                    style = AppTypography.caption.copy(color = AppColors.TextMuted),
                )
            }
        }

        when {
            isSelf -> Text(
                text = stringResource(R.string.subscriptions_you),
                style = AppTypography.label.copy(color = AppColors.TextMuted),
            )

            user.isFollowing -> BrandSecondaryButton(
                text = stringResource(R.string.subscriptions_action_unfollow),
                onClick = onToggleFollow,
                compact = true,
            )

            else -> BrandPrimaryButton(
                text = stringResource(R.string.subscriptions_action_follow),
                onClick = onToggleFollow,
                compact = true,
            )
        }
    }
}
