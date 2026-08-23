package com.yandex.avatargenerator.feature.project.ui

import android.content.Context
import android.widget.Toast
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
 * Экран публичного проекта: грузит проект по [projectId] через
 * [PublicProjectViewModel] и показывает Loading/Error/Content.
 *
 * Точка входа — тап по проекту в ленте (`Destination.PublicProject`), поэтому
 * сверху [BrandBackTopBar]: на системную кнопку «назад» как на единственный путь
 * возврата полагаться нельзя. `projectId` известен только при открытии, так
 * что передаём его параметром ([parametersOf]), как в `ProjectScreen`.
 */
@Composable
internal fun PublicProjectScreen(
    projectId: String,
    onBackClick: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onRelatedClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PublicProjectViewModel = koinViewModel { parametersOf(projectId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Тост с результатом выгрузки. Событие одноразовое (Channel во ViewModel).
    // Собираем только пока экран STARTED — событие, пришедшее в фоне, буфер
    // Channel придержит до возврата, тост не всплывёт мимо переднего плана.
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.exportEvents.collect { event ->
                Toast.makeText(context, event.message(context), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Background),
    ) {
        // Заголовок дублирует название под hero намеренно: оно уезжает со
        // скроллом, а шапка остаётся. Пока проект грузится, названия ещё нет.
        BrandBackTopBar(
            onBackClick = onBackClick,
            title = (uiState as? PublicProjectUiState.Content)?.project?.title,
        )

        when (val state = uiState) {
            PublicProjectUiState.Loading -> PublicProjectSkeleton(Modifier.weight(1f))

            PublicProjectUiState.Error -> PublicProjectCenteredBox(Modifier.weight(1f)) {
                BrandErrorState(
                    title = stringResource(R.string.public_project_error_title),
                    retryLabel = stringResource(R.string.public_project_error_retry),
                    onRetry = viewModel::retry,
                )
            }

            is PublicProjectUiState.Content -> PublicProjectContent(
                project = state.project,
                onAuthorClick = { onAuthorClick(state.project.author.id) },
                onFollowClick = viewModel::toggleFollow,
                onDownloadPngClick = { viewModel.export(ExportFormat.PNG) },
                onDownloadSvgClick = { viewModel.export(ExportFormat.SVG) },
                onRelatedClick = onRelatedClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Текст тоста по результату выгрузки. Имя формата — из ресурсов, без литералов. */
private fun ExportEvent.message(context: Context): String = when (this) {
    is ExportEvent.Success -> {
        // PNG уходит в галерею, SVG — в «Загрузки»; сообщение подсказывает, где искать.
        val template = when (format) {
            ExportFormat.PNG -> R.string.public_project_download_saved_gallery
            ExportFormat.SVG -> R.string.public_project_download_saved
        }
        context.getString(template, context.getString(format.labelRes))
    }

    is ExportEvent.Failure ->
        context.getString(R.string.public_project_download_failed, context.getString(format.labelRes))

    ExportEvent.NothingToExport ->
        context.getString(R.string.public_project_download_empty)
}

private val ExportFormat.labelRes: Int
    get() = when (this) {
        ExportFormat.PNG -> R.string.export_format_png
        ExportFormat.SVG -> R.string.export_format_svg
    }

/**
 * Вёрстка экрана без ViewModel: проект приходит параметром. Так экран рисуется в
 * `@Preview`, не ходя в репозиторий (ср. `ProjectContent`).
 */
@Composable
private fun PublicProjectContent(
    project: PublicProject,
    onAuthorClick: () -> Unit,
    onFollowClick: () -> Unit,
    onDownloadPngClick: () -> Unit,
    onDownloadSvgClick: () -> Unit,
    onRelatedClick: (String) -> Unit,
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
                onAuthorClick = onAuthorClick,
                onFollowClick = onFollowClick,
            )

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                Text(text = project.title, style = AppTypography.heading2)
                Text(
                    text = publishedLabel(project.publishedAt),
                    style = AppTypography.caption.copy(color = AppColors.TextMuted),
                )
            }

            PublicProjectActions(
                onDownloadPngClick = onDownloadPngClick,
                onDownloadSvgClick = onDownloadSvgClick,
            )

            HorizontalDivider(thickness = 1.dp, color = AppColors.Border)

            PublicProjectRelated(
                items = project.related,
                onRelatedClick = onRelatedClick,
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
    onAuthorClick: () -> Unit,
    onFollowClick: () -> Unit,
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
                .clickable(onClick = onAuthorClick),
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
        PublicProjectFollowButton(isFollowing = author.isFollowing, onClick = onFollowClick)
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
    onDownloadPngClick: () -> Unit,
    onDownloadSvgClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.s),
    ) {
        BrandPrimaryButton(
            text = stringResource(R.string.public_project_download_png),
            onClick = onDownloadPngClick,
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
            onClick = onDownloadSvgClick,
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
    onRelatedClick: (String) -> Unit,
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
                        onClick = { onRelatedClick(item.id) },
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
            project = MockPublicProjectRepository.sample(),
            onAuthorClick = {},
            onFollowClick = {},
            onDownloadPngClick = {},
            onDownloadSvgClick = {},
            onRelatedClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF222A3A, heightDp = 960)
@Composable
private fun PublicProjectContentFollowingPreview() {
    AvatarGeneratorTheme {
        val sample = MockPublicProjectRepository.sample()
        PublicProjectContent(
            project = sample.copy(author = sample.author.copy(isFollowing = true)),
            onAuthorClick = {},
            onFollowClick = {},
            onDownloadPngClick = {},
            onDownloadSvgClick = {},
            onRelatedClick = {},
        )
    }
}
