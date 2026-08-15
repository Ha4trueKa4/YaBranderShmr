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
    // Сколько сообщений уже показано. Храним именно размер, а не флаг «первый
    // проход»: по нему видно, промотка нужна из-за нового сообщения или лента
    // просто пересоздалась (поворот экрана) и трогать позицию не надо.
    var renderedCount by rememberSaveable { mutableIntStateOf(0) }

    // Ключ — размер, а не сам список: перерисовка существующего сообщения
    // (например, дозагрузка картинки) прокрутку дёргать не должна.
    LaunchedEffect(messages.size) {
        if (messages.isEmpty()) return@LaunchedEffect
        when {
            // Первое открытие: позиционируемся мгновенно, без «проезда» сверху вниз.
            renderedCount == 0 -> listState.scrollToItem(messages.lastIndex)
            // Сообщение дописали при открытом экране — прокручиваемся с анимацией.
            messages.size > renderedCount -> listState.animateScrollToItem(messages.lastIndex)
            // Размер не изменился: позицию не трогаем, её восстановил listState.
        }
        renderedCount = messages.size
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(AppSpacing.m),
        // 20dp макета округляем до токена: своего 20dp в шкале нет.
        verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
    ) {
        // key = id — чтобы Compose переиспользовал элементы при дописывании ленты.
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

                is ChatMessage.Incoming -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                ) {
                    BrandChatBubble(
                        text = message.text,
                        isOutgoing = false,
                        imageUrl = message.imageUrl,
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
