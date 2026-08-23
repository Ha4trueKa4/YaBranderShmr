package com.yandex.avatargenerator.feature.create.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.yandex.avatargenerator.feature.create.ui.theme.ChatsColors
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape

/**
 * Превью чата. Есть [previewUrl] — рисуем SVG последней версии (декодер
 * подключён в `AvatarGeneratorApplication`), нет — градиентный квадрат,
 * выбранный детерминированно по [chatId] (см.
 * [ChatsColors.rememberThumbnailGradient]): у одного и того же чата плитка
 * всегда одного цвета. Тот же градиент показываем, если ссылка есть, но
 * картинка не загрузилась (протухший URL, офлайн, битый SVG) — иначе в списке
 * остался бы серый квадрат подложки.
 *
 * Под картинкой градиента нет: у SVG фон обычно прозрачный, и он просвечивал бы
 * насквозь. Там ровная [AppColors.SurfaceVariant] — она же плейсхолдер на время
 * загрузки, как в `ProjectCard`.
 *
 * Размер задаёт вызывающий (в макете — 56.dp).
 */
@Composable
internal fun ChatThumbnail(
    chatId: String,
    modifier: Modifier = Modifier,
    previewUrl: String? = null,
) {
    val shape = RoundedCornerShape(AppShape.s)
    // Ключ по previewUrl: в LazyColumn композиция переиспользуется, и без сброса
    // новая ссылка унаследовала бы «сломанность» предыдущей.
    var isPreviewBroken by remember(previewUrl) { mutableStateOf(false) }

    if (previewUrl == null || isPreviewBroken) {
        val gradient = ChatsColors.rememberThumbnailGradient(chatId)
        Box(modifier = modifier.clip(shape).background(gradient))
    } else {
        AsyncImage(
            model = previewUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onError = { isPreviewBroken = true },
            modifier = modifier.clip(shape).background(AppColors.SurfaceVariant),
        )
    }
}
