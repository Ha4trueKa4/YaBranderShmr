package com.yandex.avatargenerator.feature.create.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.create.domain.model.Chat
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography

/**
 * Строка списка чатов: превью, название с подписью времени и стрелка «открыть».
 *
 * @param chat что показываем.
 * @param timeLabel готовая подпись времени («15 min ago») — считается снаружи
 *  ([com.yandex.avatargenerator.feature.create.ui.util.rememberRelativeTimeLabel]),
 *  чтобы строка оставалась «глупым» компонентом.
 * @param onClick открыть чат.
 */
@Composable
internal fun ChatRow(
    chat: Chat,
    timeLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppShape.m))
            .background(AppColors.Surface)
            .clickable(
                onClickLabel = stringResource(R.string.chats_open_chat_description),
                onClick = onClick,
            )
            .padding(AppSpacing.m),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChatThumbnail(chatId = chat.id, modifier = Modifier.size(56.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
        ) {
            Text(
                text = chat.title,
                style = AppTypography.body.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = timeLabel,
                style = AppTypography.caption.copy(color = AppColors.TextMuted),
                maxLines = 1,
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = AppColors.TextPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}
