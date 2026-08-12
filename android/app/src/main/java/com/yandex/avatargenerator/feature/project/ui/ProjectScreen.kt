package com.yandex.avatargenerator.feature.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.project.data.MockChatRepository
import com.yandex.avatargenerator.feature.project.ui.components.ChatInputBar
import com.yandex.avatargenerator.feature.project.ui.components.ChatTimeline
import com.yandex.avatargenerator.feature.project.ui.components.ProjectTopBar
import com.yandex.avatargenerator.ui.components.BrandErrorState
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Экран проекта: диалог с ИИ — шапка, лента сообщений и поле ввода.
 *
 * WebView-редактора сверху здесь пока нет намеренно (отложен), как и настройки
 * приватности с палитрой цветов из макета.
 *
 * `projectId` известен только при открытии экрана — передаём его параметром
 * ([parametersOf]), как в `ProfileScreen`; граф — в `feature/project/di/ProjectModule.kt`.
 *
 * @param onBackClick возврат на предыдущий экран.
 */
@Composable
internal fun ProjectScreen(
    projectId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = koinViewModel { parametersOf(projectId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProjectContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetry = viewModel::retry,
        onDraftChange = viewModel::onDraftChange,
        onSendClick = viewModel::send,
        modifier = modifier,
    )
}

/**
 * Вёрстка экрана без ViewModel: состояние приходит параметром.
 * Так экран рисуется в `@Preview`, не ходя в репозиторий.
 *
 * Нижние инсеты (системная навигация и клавиатура) экран обрабатывает сам —
 * `Scaffold` в `AvatarGeneratorApp` отдаёт этому маршруту только верхний инсет,
 * чтобы фон поля ввода доходил до края экрана. Оба нижних инсета учитывает
 * [ChatInputBar]: на корне `imePadding` не нужен и дал бы двойной отступ.
 */
@Composable
private fun ProjectContent(
    uiState: ProjectUiState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        // Пока диалог не загружен, названия проекта нет — показываем плейсхолдер,
        // чтобы шапка со стрелкой «назад» была на месте с первого кадра.
        val title = when (uiState) {
            is ProjectUiState.Content -> uiState.title
            else -> stringResource(R.string.project_title_placeholder)
        }

        ProjectTopBar(
            title = title,
            onBackClick = onBackClick,
            // TODO: экран истории версий (макет 2011:866).
            onHistoryClick = { },
            // TODO: «поделиться» проектом.
            onShareClick = { },
        )

        when (uiState) {
            ProjectUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = AppColors.Accent)
            }

            ProjectUiState.Error -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                BrandErrorState(
                    title = stringResource(R.string.project_error_title),
                    retryLabel = stringResource(R.string.project_error_retry),
                    onRetry = onRetry,
                )
            }

            is ProjectUiState.Content -> {
                // Пустая лента у нового проекта — просто пустая область,
                // поле ввода при этом доступно.
                ChatTimeline(
                    messages = uiState.messages,
                    // weight(1f) — лента забирает всё место между шапкой и вводом.
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                ChatInputBar(
                    draft = uiState.draft,
                    onDraftChange = onDraftChange,
                    onSendClick = onSendClick,
                )
            }
        }
    }
}

// --- Preview -----------------------------------------------------------------

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ProjectScreenContentPreview() {
    AvatarGeneratorTheme {
        ProjectContent(
            uiState = ProjectUiState.Content(
                title = MockChatRepository.DEFAULT_TITLE,
                messages = MockChatRepository.defaultMessages(),
                draft = "",
            ),
            onBackClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ProjectScreenEmptyChatPreview() {
    AvatarGeneratorTheme {
        ProjectContent(
            uiState = ProjectUiState.Content(
                title = "New project",
                messages = emptyList(),
                draft = "Violet logo for an AI startup",
            ),
            onBackClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ProjectScreenLoadingPreview() {
    AvatarGeneratorTheme {
        ProjectContent(
            uiState = ProjectUiState.Loading,
            onBackClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ProjectScreenErrorPreview() {
    AvatarGeneratorTheme {
        ProjectContent(
            uiState = ProjectUiState.Error,
            onBackClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
        )
    }
}
