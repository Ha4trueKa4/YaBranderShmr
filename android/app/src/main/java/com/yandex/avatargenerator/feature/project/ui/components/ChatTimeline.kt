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
 */
@Composable
internal fun ChatTimeline(
    messages: List<ChatMessage>,
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
                is ChatMessage.Incoming -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    BrandChatBubble(
                        text = stringResource(R.string.project_assistant_message),
                        isOutgoing = false,
                        imageUrl = message.previewUrl,
                        imageCacheKey = message.versionId,
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
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun ChatTimelinePreview() {
    AvatarGeneratorTheme {
        ChatTimeline(messages = MockChatRepository.defaultMessages())
    }
}
