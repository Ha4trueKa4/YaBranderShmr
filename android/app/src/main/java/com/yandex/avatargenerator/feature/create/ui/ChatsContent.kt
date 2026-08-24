package com.yandex.avatargenerator.feature.create.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.create.data.MockChatsRepository
import com.yandex.avatargenerator.feature.create.domain.model.Chat
import com.yandex.avatargenerator.feature.create.ui.components.ChatRow
import com.yandex.avatargenerator.feature.create.ui.components.ChatsRefreshErrorBanner
import com.yandex.avatargenerator.feature.create.ui.components.ChatsSkeleton
import com.yandex.avatargenerator.feature.create.ui.components.ChatsTopBar
import com.yandex.avatargenerator.feature.create.ui.util.rememberRelativeTimeLabel
import com.yandex.avatargenerator.ui.components.BrandButtonDefaults
import com.yandex.avatargenerator.ui.components.BrandEmptyState
import com.yandex.avatargenerator.ui.components.BrandErrorState
import com.yandex.avatargenerator.ui.components.BrandPrimaryButton
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Вёрстка экрана чатов без ViewModel: всё, что рисуется, приходит в [state],
 * всё, что делает пользователь, уходит одним [onIntent] — так экран рисуется
 * в `@Preview`, не ходя в репозиторий.
 */
@Composable
internal fun ChatsContent(
    state: ChatsState,
    onIntent: (ChatsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        ChatsTopBar()

        if (state.isRefreshFailed) {
            ChatsRefreshErrorBanner(onRetry = { onIntent(ChatsIntent.OnRetryClick) })
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Порядок веток и есть взаимоисключение состояний: скелетон поверх
            // всего, ошибка важнее пустоты, список — последним.
            when {
                state.isLoading -> ChatsSkeleton(Modifier.fillMaxSize())

                state.hasError -> BrandErrorState(
                    title = stringResource(R.string.chats_error_title),
                    retryLabel = stringResource(R.string.chats_error_retry),
                    onRetry = { onIntent(ChatsIntent.OnRetryClick) },
                    modifier = Modifier.align(Alignment.Center),
                )

                state.isEmpty -> BrandEmptyState(
                    title = stringResource(R.string.chats_empty_title),
                    subtitle = stringResource(R.string.chats_empty_subtitle),
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> ChatsList(
                    chats = state.chats,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            BrandPrimaryButton(
                text = stringResource(R.string.chats_new_project),
                onClick = { onIntent(ChatsIntent.OnNewProjectClick) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_plus),
                        contentDescription = null,
                        tint = AppColors.OnAccent,
                        modifier = Modifier.size(20.dp),
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = AppSpacing.m, bottom = AppSpacing.m),
            )
        }
    }
}

@Composable
private fun ChatsList(
    chats: List<Chat>,
    onIntent: (ChatsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = AppSpacing.m,
            end = AppSpacing.m,
            top = AppSpacing.s,
            bottom = AppSpacing.s + BrandButtonDefaults.Height + AppSpacing.m,
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        items(items = chats, key = { chat -> chat.id }) { chat ->
            ChatRow(
                chat = chat,
                timeLabel = chat.updatedAt?.let { rememberRelativeTimeLabel(it) },
                onClick = { onIntent(ChatsIntent.OnChatClick(chat.id)) },
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun ChatsScreenContentPreview() {
    AvatarGeneratorTheme {
        ChatsContent(
            state = ChatsState(isLoading = false, chats = MockChatsRepository.defaultChats()),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun ChatsScreenRefreshFailedPreview() {
    AvatarGeneratorTheme {
        ChatsContent(
            state = ChatsState(
                isLoading = false,
                chats = MockChatsRepository.defaultChats(),
                isRefreshFailed = true,
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun ChatsScreenEmptyPreview() {
    AvatarGeneratorTheme {
        ChatsContent(
            state = ChatsState(isLoading = false),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun ChatsScreenLoadingPreview() {
    AvatarGeneratorTheme {
        ChatsContent(
            state = ChatsState(isLoading = true),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun ChatsScreenErrorPreview() {
    AvatarGeneratorTheme {
        ChatsContent(
            state = ChatsState(isLoading = false, hasError = true),
            onIntent = {},
        )
    }
}
