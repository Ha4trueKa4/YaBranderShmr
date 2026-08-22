package com.yandex.avatargenerator.feature.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.yandex.avatargenerator.feature.project.ui.components.ProjectSkeleton
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
        onPreviewNeeded = viewModel::onPreviewNeeded,
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
    onPreviewNeeded: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
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
            ProjectUiState.Loading -> ProjectSkeleton(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )

            ProjectUiState.Error -> ProjectCenteredBox {
                BrandErrorState(
                    title = stringResource(R.string.project_error_title),
                    retryLabel = stringResource(R.string.project_error_retry),
                    onRetry = onRetry,
                )
            }

            is ProjectUiState.Content -> {
                ChatTimeline(
                    messages = uiState.messages,
                    onPreviewNeeded = onPreviewNeeded,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                ChatInputBar(
                    draft = uiState.draft,
                    onDraftChange = onDraftChange,
                    onSendClick = onSendClick,
                    isSending = uiState.isSending,
                )
            }
        }
    }
}

/**
 * Центрирующая обёртка для состояний Loading и Error — одинаковая у обоих.
 *
 * Обернуть в общий `Box` весь `when` нельзя: ветка `Content` живёт в `Column`
 * и использует `Modifier.weight(1f)`, которого в `BoxScope` нет.
 */
@Composable
private fun ProjectCenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ProjectScreenContentPreview() {
    AvatarGeneratorTheme {
        ProjectContent(
            uiState = ProjectUiState.Content(
                title = MockChatRepository.DEFAULT_TITLE,
                isPublic = false,
                messages = MockChatRepository.defaultMessages(),
                draft = "",
            ),
            onBackClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
            onPreviewNeeded = {},
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
                isPublic = false,
                messages = emptyList(),
                draft = "Violet logo for an AI startup",
            ),
            onBackClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
            onPreviewNeeded = {},
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
            onPreviewNeeded = {},
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
            onPreviewNeeded = {},
        )
    }
}
