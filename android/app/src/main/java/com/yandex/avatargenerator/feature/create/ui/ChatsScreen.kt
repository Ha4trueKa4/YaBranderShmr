package com.yandex.avatargenerator.feature.create.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yandex.avatargenerator.feature.create.data.MockChatsRepository
import com.yandex.avatargenerator.feature.create.domain.model.Chat
import com.yandex.avatargenerator.feature.create.ui.components.ChatRow
import com.yandex.avatargenerator.feature.create.ui.components.ChatsEmptyState
import com.yandex.avatargenerator.feature.create.ui.components.ChatsErrorState
import com.yandex.avatargenerator.feature.create.ui.components.ChatsSkeleton
import com.yandex.avatargenerator.feature.create.ui.components.ChatsTopBar
import com.yandex.avatargenerator.feature.create.ui.components.NewProjectButton
import com.yandex.avatargenerator.feature.create.ui.components.NewProjectButtonHeight
import com.yandex.avatargenerator.feature.create.ui.util.rememberRelativeTimeLabel
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Экран вкладки «Создать»: список чатов пользователя (чат = диалог с ИИ = проект)
 * и плавающая кнопка «New project».
 *
 * Навигация отдана наружу коллбэками — владелец графа прокинет их:
 * @param onChatClick открыть чат (он же проект) по id.
 * @param onNewProject создать новый проект.
 *
 * ViewModel создаётся дефолтной фабрикой с моком (пока без DI) — как в
 * [com.yandex.avatargenerator.feature.feed.ui.FeedScreen]. Про инсеты думать не
 * нужно: `AvatarGeneratorApp` уже отдаёт `innerPadding` в `NavHost`, а нижняя
 * панель сама обрабатывает `WindowInsets.navigationBars`.
 */
@Composable
internal fun ChatsScreen(
    onChatClick: (chatId: String) -> Unit,
    onNewProject: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatsViewModel = viewModel(factory = ChatsViewModelFactory(MockChatsRepository())),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ChatsContent(
        uiState = uiState,
        onChatClick = onChatClick,
        onNewProject = onNewProject,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

/**
 * Вёрстка экрана без ViewModel: состояние приходит параметром.
 * Так экран рисуется в `@Preview`, не ходя в репозиторий.
 */
@Composable
private fun ChatsContent(
    uiState: ChatsUiState,
    onChatClick: (chatId: String) -> Unit,
    onNewProject: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        ChatsTopBar()

        Box(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                ChatsUiState.Loading -> ChatsSkeleton(Modifier.fillMaxSize())
                ChatsUiState.Empty -> ChatsEmptyState(Modifier.fillMaxSize())
                ChatsUiState.Error -> ChatsErrorState(onRetry = onRetry, modifier = Modifier.fillMaxSize())
                is ChatsUiState.Content -> ChatsList(
                    chats = uiState.chats,
                    onChatClick = onChatClick,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            NewProjectButton(
                onClick = onNewProject,
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
    onChatClick: (chatId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = AppSpacing.m,
            end = AppSpacing.m,
            top = AppSpacing.s,
            // Снизу — обычный отступ списка плюс место под плавающую кнопку с её
            // отступом: иначе последняя строка при прокрутке до конца прячется под ней.
            bottom = AppSpacing.s + NewProjectButtonHeight + AppSpacing.m,
        ),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        // key = id — чтобы Compose переиспользовал строки при обновлении списка.
        items(items = chats, key = { chat -> chat.id }) { chat ->
            ChatRow(
                chat = chat,
                timeLabel = rememberRelativeTimeLabel(chat.updatedAt),
                onClick = { onChatClick(chat.id) },
            )
        }
    }
}

// --- Preview -----------------------------------------------------------------

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ChatsScreenContentPreview() {
    AvatarGeneratorTheme {
        ChatsContent(
            uiState = ChatsUiState.Content(MockChatsRepository.defaultChats()),
            onChatClick = {},
            onNewProject = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ChatsScreenEmptyPreview() {
    AvatarGeneratorTheme {
        ChatsContent(
            uiState = ChatsUiState.Empty,
            onChatClick = {},
            onNewProject = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ChatsScreenLoadingPreview() {
    AvatarGeneratorTheme {
        ChatsContent(
            uiState = ChatsUiState.Loading,
            onChatClick = {},
            onNewProject = {},
            onRetry = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ChatsScreenErrorPreview() {
    AvatarGeneratorTheme {
        ChatsContent(
            uiState = ChatsUiState.Error,
            onChatClick = {},
            onNewProject = {},
            onRetry = {},
        )
    }
}
