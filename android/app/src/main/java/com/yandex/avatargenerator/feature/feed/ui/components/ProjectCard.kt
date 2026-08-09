package com.yandex.avatargenerator.feature.feed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.ui.components.BrandAvatar
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import kotlin.math.abs

/**
 * Карточка проекта в masonry-ленте: превью (переменная высота) + название + автор.
 * Клик по карточке — [onClick], клик по строке автора — [onAuthorClick].
 *
 * Оформлена по дизайн-системе (оболочка как у [com.yandex.avatargenerator.ui.components.BrandCard]),
 * но с переменной высотой превью для masonry-эффекта — поэтому не переиспользует BrandCard напрямую.
 */
@Composable
fun ProjectCard(
    project: FeedProject,
    onClick: () -> Unit,
    onAuthorClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Переменная высота превью создаёт masonry-эффект. Детерминирована по id.
    // TODO: заменить на реальные размеры превью, когда бэкенд начнёт их отдавать.
    val aspectRatio = remember(project.id) {
        1.0f + (abs(project.id.hashCode()) % 90) / 100f
    }
    val shape = RoundedCornerShape(AppShape.l)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, shape)
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = project.previewUrl,
            contentDescription = project.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .background(AppColors.SurfaceVariant),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.s),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Text(
                text = project.title,
                style = AppTypography.body.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppShape.s))
                    .clickable(onClick = onAuthorClick),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandAvatar(
                    size = 24.dp,
                    imageUrl = project.author.avatarUrl,
                    initials = authorInitials(project.author.displayName),
                )
                Text(
                    text = project.author.displayName,
                    style = AppTypography.caption.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Инициалы автора для плейсхолдера аватара (когда нет avatarUrl): первые буквы
 * 1–2 слов имени — «Alice Cooper» → «AC», «Brandkit» → «B». Пустое/пробельное
 * имя → null, чтобы [BrandAvatar] показал декоративную заглушку, а не пустой круг.
 */
private fun authorInitials(name: String): String? =
    name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .takeIf { it.isNotEmpty() }
