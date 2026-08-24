package com.yandex.avatargenerator.feature.project.ui.publicproject

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.project.data.MockPublicProjectRepository
import com.yandex.avatargenerator.feature.project.domain.model.ExportFormat
import com.yandex.avatargenerator.feature.project.domain.model.PublicProject
import com.yandex.avatargenerator.feature.project.domain.model.PublicProjectAuthor
import com.yandex.avatargenerator.feature.project.domain.model.PublicProjectRelatedItem
import com.yandex.avatargenerator.feature.project.ui.components.PublicProjectSkeleton
import com.yandex.avatargenerator.feature.project.ui.util.publishedLabel
import com.yandex.avatargenerator.ui.components.BrandAvatar
import com.yandex.avatargenerator.ui.components.BrandBackTopBar
import com.yandex.avatargenerator.ui.components.BrandErrorState
import com.yandex.avatargenerator.ui.components.BrandPrimaryButton
import com.yandex.avatargenerator.ui.components.BrandTonalButton
import com.yandex.avatargenerator.ui.components.avatarInitials
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme
import com.yandex.avatargenerator.ui.theme.LocalAppColorScheme

// Декоративный градиент hero-баннера (коралл → мягкий коралл, слева направо):
// виден, пока грузится превью проекта или если превью нет. Собран целиком на
// токенах палитры. AppColors — @Composable-геттеры (зависят
// от темы), поэтому градиент собирается функцией, а не top-level val;
// remember по палитре — чтобы Brush не пересобирался на каждой рекомпозиции.
@Composable
private fun heroGradient(): Brush {
    val colors = LocalAppColorScheme.current
    return remember(colors) {
        Brush.horizontalGradient(
            listOf(colors.accent, colors.accentSoft),
        )
    }
}

// Заглушка превью карточек «More from this creator» — на токенах палитры.
@Composable
private fun previewGradient(): Brush {
    val colors = LocalAppColorScheme.current
    return remember(colors) {
        Brush.horizontalGradient(listOf(colors.accent, colors.accentSoft))
    }
}

private val HeroHeight = 300.dp
private val RelatedPreviewHeight = 100.dp
private val ActionIconSize = 18.dp

/**
 * Вёрстка экрана публичного проекта без ViewModel: всё, что рисуется, приходит
 * в [state], всё, что делает пользователь, уходит одним [onIntent] — так экран
 * рисуется в `@Preview`, не ходя в репозиторий.
 */
@Composable
internal fun PublicProjectContent(
    state: PublicProjectState,
    onIntent: (PublicProjectIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        // Заголовок дублирует название под hero намеренно: оно уезжает со
        // скроллом, а шапка остаётся. Пока проект грузится, названия ещё нет.
        BrandBackTopBar(
            onBackClick = { onIntent(PublicProjectIntent.OnBackClick) },
            title = state.project?.title,
        )

        // Порядок веток и есть взаимоисключение состояний: скелетон поверх
        // всего, ошибка важнее проекта, тело — последним.
        when {
            state.isLoading -> PublicProjectSkeleton(Modifier.weight(1f))

            state.hasError -> PublicProjectCenteredBox(Modifier.weight(1f)) {
                BrandErrorState(
                    title = stringResource(R.string.public_project_error_title),
                    retryLabel = stringResource(R.string.public_project_error_retry),
                    onRetry = { onIntent(PublicProjectIntent.OnRetryClick) },
                )
            }

            state.project != null -> PublicProjectBody(
                project = state.project,
                onIntent = onIntent,
                modifier = Modifier.weight(1f),
            )

            // Проекта нет, но и грузить его никто не начинал — рисовать нечего.
            else -> Unit
        }
    }
}

/** Собственно проект: hero, автор, действия и работы автора. */
@Composable
private fun PublicProjectBody(
    project: PublicProject,
    onIntent: (PublicProjectIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        PublicProjectHero(
            title = project.title,
            badge = project.badge,
            previewUrl = project.previewUrl,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.m),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m),
        ) {
            PublicProjectAuthorRow(
                author = project.author,
                onIntent = onIntent,
            )

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(text = project.title, style = AppTypography.heading2)
                Text(
                    text = publishedLabel(project.publishedAt),
                    style = AppTypography.caption.copy(color = AppColors.TextMuted),
                )
            }

            PublicProjectActions(onIntent = onIntent)

            HorizontalDivider(thickness = 1.dp, color = AppColors.Border)

            PublicProjectRelated(
                items = project.related,
                onIntent = onIntent,
            )
        }
    }
}

/**
 * Центрирующая обёртка для состояний Loading и Error.
 *
 * `fillMaxSize` в `modifier` вызывающей стороны здесь ожидаем — экран занимает
 * весь маршрут; отдельного `Box.fillMaxSize` не добавляем, чтобы не перетереть.
 */
@Composable
private fun PublicProjectCenteredBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Hero-баннер: превью проекта (или градиент-заглушка) со стеклянной плашкой-меткой. */
@Composable
private fun PublicProjectHero(
    title: String,
    badge: String,
    previewUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HeroHeight)
            .background(heroGradient()),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (previewUrl != null) {
            AsyncImage(
                model = previewUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // «Стекло» имитируем полупрозрачной подложкой и тонким светлым бордером —
        // backdrop-blur из макета в Compose портативно не воспроизвести.
        //
        // Метку бэкенд не отдаёт (маппер подставляет ""), а пустой Text оставлял
        // поверх превью тёмную пилюлю из одних отступов — похожую на неработающую
        // кнопку. Цвет текста здесь не из палитры: подложка тёмная в обеих темах,
        // и TextPrimary в светлой темнел до нечитаемого.
        if (badge.isNotBlank()) {
            val badgeShape = RoundedCornerShape(AppShape.full)
            Text(
                text = badge,
                style = AppTypography.caption.copy(
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier
                    .padding(AppSpacing.l)
                    .clip(badgeShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), badgeShape)
                    .padding(horizontal = AppSpacing.s, vertical = 6.dp),
            )
        }
    }
}

/** Строка автора: аватар + имя/роль (кликабельны) и кнопка подписки. */
@Composable
private fun PublicProjectAuthorRow(
    author: PublicProjectAuthor,
    onIntent: (PublicProjectIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(AppShape.s))
                .clickable { onIntent(PublicProjectIntent.OnAuthorClick) },
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandAvatar(size = 40.dp, imageUrl = author.avatarUrl, initials = avatarInitials(author.name))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = author.name,
                    style = AppTypography.body.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Роль бэкенд не отдаёт (маппер подставляет ""). Пустой Text
                // занимал вторую строку, и имя вставало выше центра — мимо
                // аватара и кнопки, которые Row центрирует по вертикали.
                if (author.role.isNotBlank()) {
                    Text(
                        text = author.role,
                        style = AppTypography.caption,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        PublicProjectFollowButton(
            isFollowing = author.isFollowing,
            onClick = { onIntent(PublicProjectIntent.OnFollowClick) },
        )
    }
}

/**
 * Компактная кнопка-пилюля подписки — по макету меньше стандартной 48-dp CTA
 * дизайн-системы, поэтому собрана точечно, только для этой строки автора.
 */
@Composable
private fun PublicProjectFollowButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppShape.full)
    val background = if (isFollowing) AppColors.Surface else AppColors.Accent
    val foreground = if (isFollowing) AppColors.TextSecondary else AppColors.OnAccent
    // Текст лежит в Box, а не красится отступами напрямую: у caption lineHeight
    // (16sp) больше кегля (12sp), и этот излишек Text распределяет по baseline —
    // подпись съезжала от центра пилюли.
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(
                if (isFollowing) Modifier.border(1.dp, AppColors.Border, shape) else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(
                if (isFollowing) R.string.public_project_following else R.string.public_project_follow,
            ),
            style = AppTypography.caption.copy(
                color = foreground,
                fontWeight = FontWeight.Bold,
                lineHeight = TextUnit.Unspecified,
            ),
        )
    }
}

/** Пара кнопок экспорта: акцентная «Download PNG» и нейтральная «Download SVG». */
@Composable
private fun PublicProjectActions(
    onIntent: (PublicProjectIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        BrandPrimaryButton(
            text = stringResource(R.string.public_project_download_png),
            onClick = { onIntent(PublicProjectIntent.OnExportClick(ExportFormat.PNG)) },
            fullWidth = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(ActionIconSize),
                )
            },
        )
        BrandTonalButton(
            text = stringResource(R.string.public_project_download_svg),
            onClick = { onIntent(PublicProjectIntent.OnExportClick(ExportFormat.SVG)) },
            fullWidth = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Code,
                    contentDescription = null,
                    modifier = Modifier.size(ActionIconSize),
                )
            },
        )
    }
}

/** Секция «More from this creator»: заголовок и сетка 2×N карточек-работ автора. */
@Composable
private fun PublicProjectRelated(
    items: List<PublicProjectRelatedItem>,
    onIntent: (PublicProjectIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        Text(
            text = stringResource(R.string.public_project_related_title),
            style = AppTypography.label.copy(fontWeight = FontWeight.Bold),
        )
        // Раскладываем по два в ряд. Списка сюда приходит немного (подборка),
        // поэтому обычные Row внутри Column, без LazyGrid — экран и так скроллится.
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                rowItems.forEach { item ->
                    PublicProjectRelatedCard(
                        item = item,
                        onClick = { onIntent(PublicProjectIntent.OnRelatedClick(item.id)) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Нечётный хвост: пустая ячейка держит ширину карточки в ряду.
                if (rowItems.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Карточка работы автора: превью (или градиент-заглушка) + название и подпись. */
@Composable
private fun PublicProjectRelatedCard(
    item: PublicProjectRelatedItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppShape.m)
    Column(
        modifier = modifier
            .clip(shape)
            .background(AppColors.Surface)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(RelatedPreviewHeight)
                .background(previewGradient()),
        ) {
            if (item.previewUrl != null) {
                AsyncImage(
                    model = item.previewUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(
            modifier = Modifier.padding(
                start = AppSpacing.s,
                end = AppSpacing.s,
                top = AppSpacing.xs,
                bottom = AppSpacing.s,
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title,
                style = AppTypography.label.copy(
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                style = AppTypography.caption,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// --- Preview -----------------------------------------------------------------

@Preview(showBackground = true, backgroundColor = 0xFF222A3A, heightDp = 960)
@Composable
private fun PublicProjectContentPreview() {
    AvatarGeneratorTheme {
        PublicProjectContent(
            state = PublicProjectState(
                isLoading = false,
                project = MockPublicProjectRepository.sample(),
            ),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A, heightDp = 960)
@Composable
private fun PublicProjectContentFollowingPreview() {
    AvatarGeneratorTheme {
        val sample = MockPublicProjectRepository.sample()
        PublicProjectContent(
            state = PublicProjectState(
                isLoading = false,
                project = sample.copy(author = sample.author.copy(isFollowing = true)),
            ),
            onIntent = {},
        )
    }
}
