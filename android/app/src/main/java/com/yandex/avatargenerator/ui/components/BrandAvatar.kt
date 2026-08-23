package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme
import com.yandex.avatargenerator.ui.theme.InterFontFamily

/**
 * Круглый аватар (см. секцию "Avatars" в Component Sandbox): изображение по [imageUrl],
 * иначе инициалы [initials] на плашке [AppColors.Surface], иначе декоративная заглушка
 * с градиентом [AppColors.AccentGradient].
 */
@Composable
fun BrandAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    imageUrl: String? = null,
    initials: String? = null
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(
                when {
                    imageUrl != null -> Modifier.background(AppColors.SurfaceVariant)
                    initials != null -> Modifier.background(AppColors.Surface)
                    else -> Modifier.background(AppColors.AccentGradient)
                }
            )
            .border(1.dp, AppColors.Border, shape),
        contentAlignment = Alignment.Center
    ) {
        when {
            imageUrl != null -> AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            initials != null -> Text(
                text = initials.take(2).uppercase(),
                color = AppColors.TextSecondary,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.4f).sp
            )
        }
    }
}

/**
 * Инициалы для плейсхолдера [BrandAvatar]: «Sarah Chen» → «SC», «Prince» → «P».
 * Пустое имя — `null`: аватар уходит на градиентную заглушку, а не на пустую плашку.
 */
fun avatarInitials(name: String): String? {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> null
        parts.size == 1 -> parts.first().take(1).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A)
@Composable
private fun BrandAvatarPreview() {
    AvatarGeneratorTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrandAvatar(size = 24.dp)
                BrandAvatar(size = 24.dp, initials = "AI")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrandAvatar(size = 40.dp)
                BrandAvatar(size = 40.dp, initials = "BK")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BrandAvatar(size = 56.dp)
                BrandAvatar(size = 56.dp, initials = "AI")
            }
        }
    }
}
