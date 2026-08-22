package com.yandex.avatargenerator.feature.profile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.profile.domain.model.UserProfile
import com.yandex.avatargenerator.ui.components.BrandAvatar
import com.yandex.avatargenerator.ui.components.BrandPrimaryButton
import com.yandex.avatargenerator.ui.components.BrandSecondaryButton
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

/**
 * Шапка профиля по макету: градиентная обложка, аватар внахлёст, имя/никнейм,
 * био, строка статистики и кнопка действия.
 *
 * Собрана из примитивов дизайн-системы ([BrandAvatar], [BrandPrimaryButton],
 * [BrandSecondaryButton], токены `AppColors/AppTypography/AppSpacing`) —
 * своих стилей/цветов/размеров не заводит.
 *
 * @param onActionClick клик по кнопке: для чужого профиля — подписка/отписка,
 *   для своего — переход к редактированию профиля.
 * @param onFollowersClick тап по счётчику подписчиков — открыть список.
 * @param onFollowingClick тап по счётчику подписок — открыть список.
 * @param onOpenSettings открыть настройки; задаётся только для своего профиля —
 *   тогда в правом верхнем углу обложки показывается кнопка-шестерёнка.
 */
@Composable
fun ProfileHeader(
    profile: UserProfile,
    isOwnProfile: Boolean,
    onActionClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Cover(avatarUrl = profile.avatarUrl, initials = initials(profile.displayName))
            if (isOwnProfile && onOpenSettings != null) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AppSpacing.xs),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings),
                        contentDescription = stringResource(R.string.settings_open_description),
                        // Акцентный фиолетовый, а не белый: на ярком циан-конце градиента
                        // (правый-верхний угол обложки) белая иконка в тёмной теме блёкнет.
                        tint = AppColors.Accent,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = AppSpacing.m,
                    end = AppSpacing.m,
                    top = AppSpacing.s,
                    bottom = AppSpacing.m,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
        ) {
            NameBlock(displayName = profile.displayName, handle = profile.handle)

            Text(
                text = profile.bio,
                style = AppTypography.body.copy(color = AppColors.TextSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            StatsRow(
                projects = profile.projectsCount,
                followers = profile.followersCount,
                following = profile.followingCount,
                onFollowersClick = onFollowersClick,
                onFollowingClick = onFollowingClick,
            )

            ProfileActionButton(
                isOwnProfile = isOwnProfile,
                isFollowing = profile.isFollowing,
                onClick = onActionClick,
            )
        }

        HorizontalDivider(color = AppColors.Border)
    }
}

/** Градиентная обложка с [BrandAvatar], наезжающим на её нижний край. */
@Composable
private fun Cover(avatarUrl: String?, initials: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(151.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(AppColors.AccentGradient),
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 89.dp)
                .clip(CircleShape)
                .background(AppColors.Background)
                .padding(3.dp),
        ) {
            BrandAvatar(size = 56.dp, imageUrl = avatarUrl, initials = initials)
        }
    }
}

@Composable
private fun NameBlock(displayName: String, handle: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
    ) {
        Text(text = displayName, style = AppTypography.heading3)
        // Никнейма нет в контракте профиля — строку не рисуем, пока не появится.
        if (handle != null) {
            Text(
                text = stringResource(R.string.profile_handle, handle),
                style = AppTypography.label.copy(color = AppColors.TextMuted),
            )
        }
    }
}

/**
 * «8 Projects · 1.2K Followers · 45 Following». Followers и Following кликабельны
 * (ведут на списки подписчиков/подписок) — поэтому не одна строка, а сегменты:
 * тап нужен ровно по счётчику, а не по всей строке.
 */
@Composable
private fun StatsRow(
    projects: Int,
    followers: Int,
    following: Int,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = AppTypography.label.copy(
        color = AppColors.TextPrimary,
        fontWeight = FontWeight.SemiBold,
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.profile_stat_projects, projects), style = style)
        StatSeparator()
        Text(
            text = stringResource(R.string.profile_stat_followers, formatCount(followers)),
            style = style,
            modifier = Modifier.statClickable(onFollowersClick),
        )
        StatSeparator()
        Text(
            text = stringResource(R.string.profile_stat_following, following),
            style = style,
            modifier = Modifier.statClickable(onFollowingClick),
        )
    }
}

@Composable
private fun StatSeparator() {
    Text(
        text = stringResource(R.string.profile_stat_separator),
        style = AppTypography.label.copy(color = AppColors.TextMuted, fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(horizontal = AppSpacing.xxs),
    )
}

/** Кликабельный счётчик без ripple: подсветка на инлайновом тексте выглядит грязно. */
@Composable
private fun Modifier.statClickable(onClick: () -> Unit): Modifier = clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick,
)

/**
 * Кнопка действия шапки, три вида по макету, все — из дизайн-системы:
 *  - свой профиль → «Edit Profile»: [BrandSecondaryButton] (outline);
 *  - чужой, подписан → «Following»: [BrandSecondaryButton] (outline);
 *  - чужой, не подписан → «Follow»: [BrandPrimaryButton] (заливка акцентом).
 */
@Composable
private fun ProfileActionButton(
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isOwnProfile -> BrandSecondaryButton(
            text = stringResource(R.string.profile_action_edit),
            onClick = onClick,
            modifier = modifier,
        )

        isFollowing -> BrandSecondaryButton(
            text = stringResource(R.string.profile_action_following),
            onClick = onClick,
            modifier = modifier,
        )

        else -> BrandPrimaryButton(
            text = stringResource(R.string.profile_action_follow),
            onClick = onClick,
            modifier = modifier,
        )
    }
}

/** Инициалы для плейсхолдера аватара: «Sarah Chen» → «SC», «Prince» → «P». */
private fun initials(displayName: String): String {
    val parts = displayName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts.first().take(1)
        else -> "${parts.first().first()}${parts.last().first()}"
    }
}

/** 1200 → «1.2K», 1_000_000 → «1M». Целые до 1000 — как есть. */
private fun formatCount(value: Int): String = when {
    value >= 1_000_000 -> trimZero(value / 1_000_000.0) + "M"
    value >= 1_000 -> trimZero(value / 1_000.0) + "K"
    else -> value.toString()
}

private fun trimZero(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
