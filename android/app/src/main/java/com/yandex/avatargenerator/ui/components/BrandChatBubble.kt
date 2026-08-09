package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme

/**
 * Пузырь сообщения чата (см. "Chat Bubbles" в Component Sandbox). Исходящее
 * сообщение ([isOutgoing] = true) — заливка [AppColors.Accent], входящее —
 * [AppColors.Surface] с рамкой. Выравнивание по краю экрана — на вызывающей
 * стороне (оберните в `Row` с нужным `horizontalArrangement`).
 */
@Composable
fun BrandChatBubble(
    text: String,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier,
    imageUrl: String? = null
) {
    val shape: Shape = RoundedCornerShape(AppShape.l)
    val background = if (isOutgoing) AppColors.Accent else AppColors.Surface
    val foreground = if (isOutgoing) AppColors.OnAccent else AppColors.TextPrimary

    Column(
        modifier = modifier
            .widthIn(max = 280.dp)
            .clip(shape)
            .background(background)
            .then(if (isOutgoing) Modifier else Modifier.border(1.dp, AppColors.Border, shape))
            .padding(PaddingValues(horizontal = AppSpacing.m, vertical = AppSpacing.s)),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s)
    ) {
        Text(text = text, style = AppTypography.body.copy(color = foreground))
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(AppShape.s))
                    .background(AppColors.SurfaceVariant)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14)
@Composable
private fun BrandChatBubblePreview() {
    AvatarGeneratorTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrandChatBubble(
                text = "Can you generate a modern violet branding kit for my AI startup?",
                isOutgoing = true
            )
            BrandChatBubble(
                text = "I've initialized the generation! Here is a sneak peek of your Quantum branding asset:",
                isOutgoing = false,
                imageUrl = "https://placehold.co/248x100"
            )
        }
    }
}
