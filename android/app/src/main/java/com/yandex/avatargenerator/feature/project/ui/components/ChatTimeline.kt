package com.yandex.avatargenerator.feature.project.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.project.data.MockChatRepository
import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage
import com.yandex.avatargenerator.feature.project.ui.util.rememberMessageTimeLabel
import com.yandex.avatargenerator.ui.components.BrandChatBubble
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Лента сообщений проекта. Каждый вид сообщения рисуется своим элементом:
 * пузыри по краям экрана, системные события — пилюлей и разделителем версии.
 *
 * Открывается сразу на последнем сообщении; при появлении нового сообщения
 * прокручивается к нему с анимацией.
 *
 * @param onPreviewNeeded вызывается, когда пузырь ответа ИИ без картинки дошёл
 *  до экрана: ссылку на превью прошлой версии запрашивают по требованию, а не
 *  пачкой при открытии. Повторные вызовы по одной версии отсекает ViewModel.
 * @param onRetry «Повторить» на пузыре провалившейся генерации; параметр — id
 *  этого пузыря, промпт для повтора ViewModel найдёт над ним сама.
 * @param onPreviewClick тап по картинке ответа ИИ — ведёт на витрину проекта,
 *  где его скачивают в PNG/SVG. Своих кнопок выгрузки у чата нет.
 */
@Composable
internal fun ChatTimeline(
    messages: List<ChatMessage>,
    onPreviewNeeded: (String) -> Unit,
    onRetry: (String) -> Unit,
    onPreviewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var renderedCount by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(messages.size) {
        if (messages.isEmpty()) return@LaunchedEffect
        when {
            renderedCount == 0 -> listState.scrollToItem(messages.lastIndex)
            messages.size > renderedCount -> listState.animateScrollToItem(messages.lastIndex)
        }
        renderedCount = messages.size
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(AppSpacing.m),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        items(items = messages, key = { message -> message.id }) { message ->
            when (message) {
                is ChatMessage.Outgoing -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    BrandChatBubble(
                        text = message.text,
                        isOutgoing = true,
                        timeLabel = rememberMessageTimeLabel(message.sentAt),
                    )
                }

                // Своего текста у ответа ИИ нет — в истории лежит только версия.
                is ChatMessage.Incoming -> {
                    if (message.previewUrl == null) {
                        LaunchedEffect(message.versionId) { onPreviewNeeded(message.versionId) }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        BrandChatBubble(
                            text = stringResource(R.string.project_assistant_message),
                            isOutgoing = false,
                            imageUrl = message.previewUrl,
                            imageCacheKey = message.versionId,
                            imageContentDescription = stringResource(R.string.project_download_description),
                            onImageClick = onPreviewClick,
                            timeLabel = rememberMessageTimeLabel(message.sentAt),
                        )
                    }
                }

                is ChatMessage.Pending -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    ChatPendingBubble()
                }

                is ChatMessage.Failed -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    ChatFailedBubble(
                        text = message.text,
                        onRetry = { onRetry(message.id) },
                        timeLabel = rememberMessageTimeLabel(message.sentAt),
                    )
                }

                is ChatMessage.System.ProjectCreated -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    ChatSystemTag(text = stringResource(R.string.project_system_created))
                }

                is ChatMessage.System.VersionSaved -> ChatVersionDivider(
                    text = stringResource(R.string.project_system_version_saved, message.version),
                )

                is ChatMessage.System.GenerationStillRunning -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    ChatSystemTag(
                        text = stringResource(R.string.project_generation_still_running),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun ChatTimelinePreview() {
    AvatarGeneratorTheme {
        ChatTimeline(
            messages = MockChatRepository.defaultMessages(),
            onPreviewNeeded = {},
            onRetry = {},
            onPreviewClick = {},
        )
    }
}
