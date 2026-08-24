package com.yandex.avatargenerator.feature.subscription.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.subscription.domain.model.SubscriptionUser
import com.yandex.avatargenerator.feature.subscription.ui.components.SubscriptionUserRow
import com.yandex.avatargenerator.ui.components.BrandEmptyState
import com.yandex.avatargenerator.ui.components.BrandErrorState
import com.yandex.avatargenerator.ui.components.BrandListItemSkeleton
import com.yandex.avatargenerator.ui.components.BrandTopBar
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Вёрстка экрана подписок без ViewModel: всё, что рисуется, приходит в [state],
 * всё, что делает пользователь, уходит одним [onIntent] — так экран рисуется
 * в `@Preview`, не ходя в сеть.
 */
@Composable
internal fun SubscriptionsContent(
    state: SubscriptionsState,
    onIntent: (SubscriptionsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        BrandTopBar(
            title = stringResource(
                when (state.tab) {
                    SubscriptionsTab.Followers -> R.string.subscriptions_title_followers
                    SubscriptionsTab.Following -> R.string.subscriptions_title_following
                },
            ),
            backContentDescription = stringResource(R.string.subscriptions_back_description),
            onBackClick = { onIntent(SubscriptionsIntent.OnBackClick) },
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            // Порядок веток и есть взаимоисключение состояний: скелетон поверх
            // всего, ошибка важнее пустоты, список — последним.
            when {
                state.isLoading -> LoadingList(modifier = Modifier.fillMaxSize())

                state.hasError -> BrandErrorState(
                    title = stringResource(R.string.subscriptions_error_title),
                    retryLabel = stringResource(R.string.subscriptions_error_retry),
                    onRetry = { onIntent(SubscriptionsIntent.OnRetryClick) },
                    modifier = Modifier.align(Alignment.Center),
                )

                state.isEmpty -> EmptyList(
                    tab = state.tab,
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> UserList(state = state, onIntent = onIntent)
            }
        }
    }
}

@Composable
private fun UserList(
    state: SubscriptionsState,
    onIntent: (SubscriptionsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppSpacing.m),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        items(items = state.users, key = { it.id }) { user ->
            SubscriptionUserRow(
                user = user,
                isSelf = user.id == state.currentUserId,
                onClick = { onIntent(SubscriptionsIntent.OnUserClick(user.id)) },
                onToggleFollow = { onIntent(SubscriptionsIntent.OnFollowClick(user)) },
            )
        }
    }
}

@Composable
private fun LoadingList(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = AppSpacing.m),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        repeat(SKELETON_ROWS) {
            BrandListItemSkeleton(modifier = Modifier.padding(top = AppSpacing.s))
        }
    }
}

@Composable
private fun EmptyList(tab: SubscriptionsTab, modifier: Modifier = Modifier) {
    BrandEmptyState(
        title = stringResource(
            when (tab) {
                SubscriptionsTab.Followers -> R.string.subscriptions_empty_followers_title
                SubscriptionsTab.Following -> R.string.subscriptions_empty_following_title
            },
        ),
        subtitle = stringResource(
            when (tab) {
                SubscriptionsTab.Followers -> R.string.subscriptions_empty_followers_subtitle
                SubscriptionsTab.Following -> R.string.subscriptions_empty_following_subtitle
            },
        ),
        modifier = modifier,
    )
}

private const val SKELETON_ROWS = 8

// --- Preview ------------------------------------------------------------------
// Данные для превью пишем здесь: литералы в @Preview конвенция допускает.

private val previewUsers = listOf(
    SubscriptionUser(
        id = "u1",
        displayName = "Sarah Chen",
        avatarUrl = null,
        followersCount = 1_284,
        isFollowing = false,
    ),
    SubscriptionUser(
        id = "u2",
        displayName = "Alex Rivera",
        avatarUrl = null,
        followersCount = 340,
        isFollowing = true,
    ),
)

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun SubscriptionsContentListPreview() {
    AvatarGeneratorTheme {
        SubscriptionsContent(
            state = SubscriptionsState(
                tab = SubscriptionsTab.Followers,
                isLoading = false,
                users = previewUsers,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun SubscriptionsContentLoadingPreview() {
    AvatarGeneratorTheme {
        SubscriptionsContent(
            state = SubscriptionsState(tab = SubscriptionsTab.Following),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun SubscriptionsContentEmptyPreview() {
    AvatarGeneratorTheme {
        SubscriptionsContent(
            state = SubscriptionsState(tab = SubscriptionsTab.Following, isLoading = false),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun SubscriptionsContentErrorPreview() {
    AvatarGeneratorTheme {
        SubscriptionsContent(
            state = SubscriptionsState(
                tab = SubscriptionsTab.Followers,
                isLoading = false,
                hasError = true,
            ),
            onIntent = {},
        )
    }
}
