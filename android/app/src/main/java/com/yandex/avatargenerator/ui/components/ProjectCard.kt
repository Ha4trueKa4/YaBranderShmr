package com.yandex.avatargenerator.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import kotlin.math.abs

/**
 * Карточка проекта для masonry-сеток (лента, профиль): превью переменной высоты +
 * название + опциональная кликабельная строка автора (аватар + имя).
 *
 * Домен-агностична — принимает примитивы, каждый экран мапит свою модель. Оболочка
 * оформлена как у [BrandCard], но с переменной высотой превью и интерактивной строкой
 * автора (отдельный [onAuthorClick]), которых у BrandCard нет.
 *
 * @param previewUrl превью проекта; null — плашка-заглушка [AppColors.SurfaceVariant].
 * @param authorName имя автора; null — строка автора не показывается (на профиле
 *   владелец уже в шапке, дублировать в каждой карточке не нужно).
 * @param authorAvatarUrl аватар автора; null — инициалы по [authorName] через [BrandAvatar].
 * @param aspectRatio пропорции превью (ширина:высота). Для masonry высоту варьируют
 *   через [masonryAspectRatio], пока бэкенд не отдаёт реальные размеры превью.
 * @param onClick клик по карточке (открыть проект).
 * @param onAuthorClick клик по строке автора (открыть профиль автора); null — строка
 *   не кликабельна.
 */
@Composable
fun ProjectCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    authorName: String? = null,
    onAuthorClick: (() -> Unit)? = null,
    previewUrl: String? = null,
    authorAvatarUrl: String? = null,
    aspectRatio: Float = 1.3f,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .brandSurface(RoundedCornerShape(AppShape.l))
            .clickable(onClick = onClick),
    ) {
        AsyncImage(
            model = previewUrl,
            contentDescription = title,
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
                text = title,
                style = AppTypography.body.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (authorName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppShape.s))
                        .then(if (onAuthorClick != null) Modifier.clickable(onClick = onAuthorClick) else Modifier),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrandAvatar(
                        size = 24.dp,
                        imageUrl = authorAvatarUrl,
                        initials = authorInitials(authorName),
                    )
                    Text(
                        text = authorName,
                        style = AppTypography.caption.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Плейсхолдерные пропорции превью для masonry-эффекта, пока бэкенд не отдаёт реальные
 * размеры. Детерминировано по [seed] (обычно id проекта), диапазон ~1.0..1.9.
 */
fun masonryAspectRatio(seed: String): Float = 1.0f + (abs(seed.hashCode()) % 90) / 100f

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
