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
import com.yandex.avatargenerator.feature.project.domain.model.GenerationModel
import com.yandex.avatargenerator.feature.project.ui.components.ChatInputBar
import com.yandex.avatargenerator.feature.project.ui.components.ChatTimeline
import com.yandex.avatargenerator.feature.project.ui.components.ModelPickerSheet
import com.yandex.avatargenerator.feature.project.ui.components.ProjectSkeleton
import com.yandex.avatargenerator.feature.project.ui.components.ProjectTopBar
import com.yandex.avatargenerator.feature.project.ui.components.ProjectVisibilitySheet
import com.yandex.avatargenerator.feature.project.ui.util.modelTitle
import com.yandex.avatargenerator.ui.components.BrandErrorState
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Экран проекта: диалог с ИИ — шапка, лента сообщений и поле ввода.
 *
 * WebView-редактора сверху здесь пока нет намеренно (отложен). Видимость проекта
 * меняется чипом в шапке, модель генерации — чипом над полем ввода; оба
 * открывают свою шторку выбора.
 *
 * `projectId` известен только при открытии экрана — передаём его параметром
 * ([parametersOf]), как в `ProfileScreen`; граф — в `feature/project/di/ProjectModule.kt`.
 *
 * @param onBackClick возврат на предыдущий экран.
 * @param onDownloadClick тап по картинке в ответе ИИ — переход на витрину
 *  проекта, откуда его скачивают в PNG/SVG: своих кнопок выгрузки у чата нет.
 */
@Composable
internal fun ProjectScreen(
    projectId: String,
    onBackClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = koinViewModel { parametersOf(projectId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProjectContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onDownloadClick = onDownloadClick,
        onRetry = viewModel::retry,
        onDraftChange = viewModel::onDraftChange,
        onSendClick = viewModel::send,
        onPreviewNeeded = viewModel::onPreviewNeeded,
        onRetryGeneration = viewModel::retryGeneration,
        onVisibilityChange = viewModel::setVisibility,
        onModelPickerOpen = viewModel::openModelPicker,
        onModelPickerDismiss = viewModel::dismissModelPicker,
        onModelSelect = viewModel::selectModel,
        onModelsRetry = viewModel::retryModels,
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
    onDownloadClick: () -> Unit,
    onRetry: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onPreviewNeeded: (String) -> Unit,
    onRetryGeneration: (String) -> Unit,
    onVisibilityChange: (Boolean) -> Unit,
    onModelPickerOpen: () -> Unit,
    onModelPickerDismiss: () -> Unit,
    onModelSelect: (String) -> Unit,
    onModelsRetry: () -> Unit,
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

        // Видимость этой шторки, в отличие от соседней, живёт в состоянии VM:
        // так её не придётся распутывать при переезде экрана на MVI.
        if (content != null && content.modelPicker.isSheetVisible) {
            ModelPickerSheet(
                state = content.modelPicker,
                onSelect = onModelSelect,
                onRetry = onModelsRetry,
                onDismiss = onModelPickerDismiss,
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
                    onPreviewClick = onDownloadClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                ChatInputBar(
                    draft = uiState.draft,
                    onDraftChange = onDraftChange,
                    onSendClick = onSendClick,
                    modelTitle = modelTitle(uiState.modelPicker.selectedModelId),
                    onModelClick = onModelPickerOpen,
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
            onDownloadClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
            onPreviewNeeded = {},
            onRetryGeneration = {},
            onVisibilityChange = {},
            onModelPickerOpen = {},
            onModelPickerDismiss = {},
            onModelSelect = {},
            onModelsRetry = {},
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
            onDownloadClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
            onPreviewNeeded = {},
            onRetryGeneration = {},
            onVisibilityChange = {},
            onModelPickerOpen = {},
            onModelPickerDismiss = {},
            onModelSelect = {},
            onModelsRetry = {},
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
            onDownloadClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
            onPreviewNeeded = {},
            onRetryGeneration = {},
            onVisibilityChange = {},
            onModelPickerOpen = {},
            onModelPickerDismiss = {},
            onModelSelect = {},
            onModelsRetry = {},
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
            onDownloadClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
            onPreviewNeeded = {},
            onRetryGeneration = {},
            onVisibilityChange = {},
            onModelPickerOpen = {},
            onModelPickerDismiss = {},
            onModelSelect = {},
            onModelsRetry = {},
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
            onDownloadClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
            onPreviewNeeded = {},
            onRetryGeneration = {},
            onVisibilityChange = {},
            onModelPickerOpen = {},
            onModelPickerDismiss = {},
            onModelSelect = {},
            onModelsRetry = {},
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
            onDownloadClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
            onPreviewNeeded = {},
            onRetryGeneration = {},
            onVisibilityChange = {},
            onModelPickerOpen = {},
            onModelPickerDismiss = {},
            onModelSelect = {},
            onModelsRetry = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun ProjectScreenModelPickerPreview() {
    AvatarGeneratorTheme {
        ProjectContent(
            uiState = ProjectUiState.Content(
                title = MockChatRepository.DEFAULT_TITLE,
                isPublic = false,
                messages = MockChatRepository.defaultMessages(),
                modelPicker = ModelPickerState(
                    models = listOf(
                        GenerationModel(GenerationModel.DefaultId, owner = "DeepSeek"),
                        GenerationModel("gpt-5.6-luna", owner = "OpenAI"),
                    ),
                    isSheetVisible = true,
                ),
            ),
            onBackClick = {},
            onRetry = {},
            onDraftChange = {},
            onSendClick = {},
            onPreviewNeeded = {},
            onRetryGeneration = {},
            onVisibilityChange = {},
            onModelPickerOpen = {},
            onModelPickerDismiss = {},
            onModelSelect = {},
            onModelsRetry = {},
        )
    }
}
