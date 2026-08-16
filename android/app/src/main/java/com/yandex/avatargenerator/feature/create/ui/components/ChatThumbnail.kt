package com.yandex.avatargenerator.feature.create.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.yandex.avatargenerator.feature.create.ui.theme.ChatsColors
import com.yandex.avatargenerator.ui.theme.AppShape

/**
 * Превью чата — градиентный квадрат. Реального изображения проекта бэкенд пока
 * не отдаёт, поэтому градиент выбирается детерминированно по [chatId]
 * (см. [ChatsColors.rememberThumbnailGradient]): у одного и того же чата
 * плитка всегда одного цвета.
 *
 * Размер задаёт вызывающий (в макете — 56.dp).
 */
@Composable
internal fun ChatThumbnail(
    chatId: String,
    modifier: Modifier = Modifier,
) {
    val gradient = ChatsColors.rememberThumbnailGradient(chatId)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppShape.s))
            .background(gradient),
    )
}
