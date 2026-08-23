package com.yandex.avatargenerator.feature.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.ui.components.BrandChatBubble
import com.yandex.avatargenerator.ui.components.BrandSkeletonBox
import com.yandex.avatargenerator.ui.components.rememberBrandShimmerBrush
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/** Радиус «хвостика» входящего пузыря — тот же, что у [BrandChatBubble]. */
private val TailCorner = 4.dp

/** Размеры плашек внутри скелетона: строка подписи и место под картинку. */
private val CaptionWidth = 180.dp
private val CaptionHeight = 14.dp
private val PreviewWidth = 240.dp
private val PreviewHeight = 128.dp

/** Форма входящего пузыря: скруглённый со всех сторон, кроме хвостика слева. */
private val IncomingBubbleShape = RoundedCornerShape(
    topStart = AppShape.l,
    topEnd = AppShape.l,
    bottomEnd = AppShape.l,
    bottomStart = TailCorner,
)

/**
 * Скелетон ответа ИИ на время генерации: пузырь входящей формы с мерцающими
 * плашками вместо подписи и картинки.
 *
 * Отдельно от [ProjectSkeleton] (тот — на первичную загрузку всего экрана):
 * здесь пузырь ровно один и стоит в живой ленте. Примитивы общие — свой shimmer
 * фича не заводит.
 */
@Composable
internal fun ChatPendingBubble(modifier: Modifier = Modifier) {
    val brush = rememberBrandShimmerBrush()
    Column(
        modifier = modifier
            .clip(IncomingBubbleShape)
            .background(AppColors.SurfaceVariant)
            .padding(PaddingValues(horizontal = AppSpacing.m, vertical = AppSpacing.s)),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        BrandSkeletonBox(
            modifier = Modifier
                .width(CaptionWidth)
                .height(CaptionHeight),
            brush = brush,
        )
        BrandSkeletonBox(
            modifier = Modifier
                .width(PreviewWidth)
                .height(PreviewHeight),
            brush = brush,
            shape = RoundedCornerShape(AppShape.s),
        )
    }
}

/**
 * Пузырь провалившейся генерации: текст ошибки и кнопка «Повторить».
 *
 * @param text текст с сервера; `null` — сервер его не прислал, подставляем свой.
 * @param onRetry повтор промпта, стоящего над этим пузырём.
 */
@Composable
internal fun ChatFailedBubble(
    text: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    timeLabel: String? = null,
) {
    BrandChatBubble(
        text = text ?: stringResource(R.string.project_generation_failed),
        isOutgoing = false,
        modifier = modifier,
        timeLabel = timeLabel,
        textColor = AppColors.Error,
        action = {
            Text(
                text = stringResource(R.string.project_generation_retry),
                style = AppTypography.body.copy(
                    color = AppColors.Accent,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRetry,
                ),
            )
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun ChatGenerationBubblesPreview() {
    AvatarGeneratorTheme {
        Column(
            modifier = Modifier.padding(AppSpacing.m),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.l),
        ) {
            ChatPendingBubble()
            ChatFailedBubble(
                text = "Generation service is temporarily unavailable. Please try again later.",
                onRetry = {},
                timeLabel = "11:35",
            )
            // Сервер прислал ошибку без текста — подпись берётся из strings.xml.
            ChatFailedBubble(text = null, onRetry = {}, timeLabel = "11:36")
        }
    }
}
