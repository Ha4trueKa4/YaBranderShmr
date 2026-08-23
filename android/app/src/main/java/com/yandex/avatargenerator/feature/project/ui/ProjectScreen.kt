package com.yandex.avatargenerator.feature.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.yandex.avatargenerator.feature.project.ui.components.ProjectVisibilitySheet
import com.yandex.avatargenerator.ui.components.BrandErrorState
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Экран проекта: диалог с ИИ — шапка, лента сообщений и поле ввода.
 *
 * WebView-редактора сверху здесь пока нет намеренно (отложен). Видимость проекта
 * меняется чипом в шапке, открывающим шторку выбора.
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
        onRetryGeneration = viewModel::retryGeneration,
        onVisibilityChange = viewModel::setVisibility,
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
    onRetryGeneration: (String) -> Unit,
    onVisibilityChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Чип видимости есть только в состоянии Content, так что и шторка открывается
    // лишь там — этого достаточно, отдельного сброса при смене состояния не нужно.
    var visibilitySheetVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        val content = uiState as? ProjectUiState.Content
        val title = content?.title ?: stringResource(R.string.project_title_placeholder)

        ProjectTopBar(
            title = title,
            isPublic = content?.isPublic,
            onVisibilityClick = { visibilitySheetVisible = true },
            onBackClick = onBackClick,
            // TODO: экран истории версий (макет 2011:866).
            onHistoryClick = { },
            // TODO: «поделиться» проектом.
            onShareClick = { },
        )

        if (visibilitySheetVisible && content != null) {
            ProjectVisibilitySheet(
                isPublic = content.isPublic,
                onSelect = { isPublic ->
                    onVisibilityChange(isPublic)
                    visibilitySheetVisible = false
                },
                onDismiss = { visibilitySheetVisible = false },
            )
        }

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
                    onRetry = onRetryGeneration,
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

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
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
            onRetryGeneration = {},
            onVisibilityChange = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun ProjectScreenEmptyChatPreview() {
    AvatarGeneratorTheme {
        ProjectContent(
            uiState = ProjectUiState.Content(
                title = "New project",
                isPublic = false,
                messages = emptyList(),
                draft = "Coral logo for an AI startup",
            ),
            onBackClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
            onPreviewNeeded = {},
            onRetryGeneration = {},
            onVisibilityChange = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
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
            onRetryGeneration = {},
            onVisibilityChange = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun ProjectScreenGeneratingPreview() {
    AvatarGeneratorTheme {
        ProjectContent(
            uiState = ProjectUiState.Content(
                title = MockChatRepository.DEFAULT_TITLE,
                isPublic = false,
                messages = MockChatRepository.generatingMessages(),
                draft = "",
                isSending = true,
            ),
            onBackClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
            onPreviewNeeded = {},
            onRetryGeneration = {},
            onVisibilityChange = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun ProjectScreenFailedGenerationPreview() {
    AvatarGeneratorTheme {
        ProjectContent(
            uiState = ProjectUiState.Content(
                title = MockChatRepository.DEFAULT_TITLE,
                isPublic = false,
                messages = MockChatRepository.failedMessages(),
                draft = "",
            ),
            onBackClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
            onPreviewNeeded = {},
            onRetryGeneration = {},
            onVisibilityChange = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
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
            onRetryGeneration = {},
            onVisibilityChange = {},
        )
    }
}
