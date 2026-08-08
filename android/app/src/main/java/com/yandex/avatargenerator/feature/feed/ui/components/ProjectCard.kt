package com.yandex.avatargenerator.feature.feed.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yandex.avatargenerator.feature.feed.domain.model.FeedProject
import com.yandex.avatargenerator.feature.feed.ui.theme.FeedColors
import kotlin.math.abs

/**
 * Карточка проекта в masonry-ленте: превью (переменная высота) + название + автор.
 * Клик по карточке — [onClick], клик по строке автора — [onAuthorClick].
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FeedColors.Surface)
            .border(1.dp, FeedColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = project.previewUrl,
            contentDescription = project.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .background(FeedColors.SurfaceVariant),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            androidx.compose.material3.Text(
                text = project.title,
                color = FeedColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAuthorClick),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AuthorAvatar(
                    url = project.author.avatarUrl,
                    modifier = Modifier.size(24.dp),
                )
                androidx.compose.material3.Text(
                    text = project.author.displayName,
                    color = FeedColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Аватар автора: изображение по [url] либо круг-плейсхолдер, если url отсутствует
 * (или пока грузится).
 */
@Composable
private fun AuthorAvatar(url: String?, modifier: Modifier = Modifier) {
    val avatarModifier = modifier
        .clip(CircleShape)
        .background(FeedColors.SurfaceVariant)
    if (url == null) {
        Box(avatarModifier)
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = avatarModifier,
        )
    }
}
