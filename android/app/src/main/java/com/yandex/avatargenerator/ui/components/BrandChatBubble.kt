package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/** Радиус «хвостика» пузыря у края собеседника — точечное значение вне шкалы AppShape. */
private val TailCorner = 4.dp

/** Минимальная высота картинки в пузыре: ниже не сжимаем, выше растём под пропорции. */
private val MinImageHeight = 100.dp

/** Доля высоты экрана, за которую картинке расти нельзя. */
private const val MaxImageHeightFraction = 0.5f

/**
 * Пузырь сообщения чата (см. "Chat Bubbles" в Component Sandbox). Исходящее
 * сообщение ([isOutgoing] = true) — заливка [AppColors.Accent], входящее —
 * [AppColors.SurfaceVariant]. Нижний угол со стороны своего края экрана скруглён
 * слабее — это «хвостик» пузыря из макета. Выравнивание по краю экрана — на
 * вызывающей стороне (оберните в `Row` с нужным `horizontalArrangement`).
 *
 * @param timeLabel готовая подпись времени («11:32»), рисуется справа внизу;
 *  `null` — времени нет (например, у системных подсказок).
 * @param imageCacheKey стабильный ключ кэша картинки. Нужен, когда [imageUrl] —
 *  presigned-ссылка: она новая при каждом запросе, и по самому URL Coil грузил
 *  бы одну и ту же картинку заново на каждом обновлении ленты.
 * @param textColor цвет текста поверх заливки. По умолчанию — контрастный к ней;
 *  переопределяют, чтобы подкрасить сообщение (например, ошибку генерации).
 * @param action слот под текстом: место для действия по сообщению («Повторить»).
 *  Живёт внутри пузыря, чтобы кнопка была явно привязана к нему, а не висела
 *  отдельной строкой в ленте.
 */
@Composable
fun BrandChatBubble(
    text: String,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    imageCacheKey: String? = null,
    timeLabel: String? = null,
    textColor: Color? = null,
    action: (@Composable ColumnScope.() -> Unit)? = null
) {
    val shape: Shape = RoundedCornerShape(
        topStart = AppShape.l,
        topEnd = AppShape.l,
        bottomEnd = if (isOutgoing) TailCorner else AppShape.l,
        bottomStart = if (isOutgoing) AppShape.l else TailCorner
    )
    val background = if (isOutgoing) AppColors.Accent else AppColors.SurfaceVariant
    val foreground = textColor
        ?: if (isOutgoing) AppColors.OnAccent else AppColors.TextPrimary
    val timeColor = if (isOutgoing) AppColors.OnAccent.copy(alpha = 0.7f) else AppColors.TextMuted

    Column(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(shape)
            .background(background)
            .padding(PaddingValues(horizontal = AppSpacing.m, vertical = AppSpacing.s))
    ) {
        Text(text = text, style = AppTypography.body.copy(color = foreground))
        if (imageUrl != null) {
            val screenHeight = LocalWindowInfo.current.containerSize.height
            val maxImageHeight = with(LocalDensity.current) {
                (screenHeight * MaxImageHeightFraction).toDp()
            }.coerceAtLeast(MinImageHeight)
            Spacer(modifier = Modifier.height(AppSpacing.s))
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(imageUrl)
                    .memoryCacheKey(imageCacheKey)
                    .diskCacheKey(imageCacheKey)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinImageHeight, max = maxImageHeight)
                    .clip(RoundedCornerShape(AppShape.s))
                    .background(AppColors.SurfaceVariant)
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.height(AppSpacing.xs))
            action()
        }
        if (timeLabel != null) {
            Spacer(modifier = Modifier.height(AppSpacing.xxs))
            Text(
                text = timeLabel,
                style = AppTypography.caption.copy(color = timeColor),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun BrandChatBubblePreview() {
    AvatarGeneratorTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrandChatBubble(
                text = "Can you generate a modern coral branding kit for my AI startup?",
                isOutgoing = true,
                timeLabel = "11:32"
            )
            BrandChatBubble(
                text = "I've initialized the generation! Here is a sneak peek of your Quantum branding asset:",
                isOutgoing = false,
                imageUrl = "https://placehold.co/248x100",
                timeLabel = "11:33"
            )
            BrandChatBubble(
                text = "И квадратная аватарка — пузырь растёт под её пропорции:",
                isOutgoing = false,
                imageUrl = "https://placehold.co/512x512",
                timeLabel = "11:34"
            )
        }
    }
}
