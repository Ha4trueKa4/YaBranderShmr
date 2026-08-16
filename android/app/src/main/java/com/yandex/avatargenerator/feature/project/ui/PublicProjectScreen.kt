package com.yandex.avatargenerator.feature.project.ui

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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.feature.project.data.MockPublicProjectRepository
import com.yandex.avatargenerator.feature.project.domain.model.PublicProject
import com.yandex.avatargenerator.feature.project.domain.model.PublicProjectAuthor
import com.yandex.avatargenerator.feature.project.domain.model.PublicProjectRelatedItem
import com.yandex.avatargenerator.feature.project.ui.components.PublicProjectSkeleton
import com.yandex.avatargenerator.feature.project.ui.util.publishedLabel
import com.yandex.avatargenerator.ui.components.BrandAvatar
import com.yandex.avatargenerator.ui.components.BrandErrorState
import com.yandex.avatargenerator.ui.components.BrandPrimaryButton
import com.yandex.avatargenerator.ui.components.BrandTonalButton
import com.yandex.avatargenerator.ui.theme.AppColors
import com.yandex.avatargenerator.ui.theme.AppShape
import com.yandex.avatargenerator.ui.theme.AppSpacing
import com.yandex.avatargenerator.ui.theme.AppTypography
import com.yandex.avatargenerator.ui.theme.AvatarGeneratorTheme
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val HeroGradient = Brush.horizontalGradient(
    listOf(AppColors.Accent, AppColors.AccentCyan, AppColors.AccentPink),
)

private val PreviewGradient = Brush.horizontalGradient(
    listOf(AppColors.Accent, AppColors.AccentCyan),
)

private val HeroHeight = 300.dp
private val RelatedPreviewHeight = 100.dp
private val ActionIconSize = 18.dp

/**
 * Экран публичного проекта: грузит проект по [projectId] через
 * [PublicProjectViewModel] и показывает Loading/Error/Content.
 *
 * Точка входа — тап по проекту в ленте (`Destination.PublicProject`). Публичный
 * проект открывается по прямой ссылке, поэтому шапки с «назад» в макете нет —
 * возврат делает системная кнопка. `projectId` известен только при открытии, так
 * что передаём его параметром ([parametersOf]), как в `ProjectScreen`.
 */
@Composable
internal fun PublicProjectScreen(
    projectId: String,
    onAuthorClick: (String) -> Unit,
    onRelatedClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PublicProjectViewModel = koinViewModel { parametersOf(projectId) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        PublicProjectUiState.Loading -> PublicProjectSkeleton(modifier)

        PublicProjectUiState.Error -> PublicProjectCenteredBox(modifier) {
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
            // TODO: экспорт PNG/SVG — пробросить в редактор/бэкенд, когда появится.
            onDownloadPngClick = { },
            onDownloadSvgClick = { },
            onRelatedClick = onRelatedClick,
            modifier = modifier,
        )
    }
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
            .background(HeroGradient),
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
        val badgeShape = RoundedCornerShape(AppShape.full)
        Text(
            text = badge,
            style = AppTypography.caption.copy(
                color = AppColors.TextPrimary,
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
            BrandAvatar(size = 40.dp, imageUrl = author.avatarUrl, initials = author.name)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = author.name,
                    style = AppTypography.body.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = author.role,
                    style = AppTypography.caption,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
    Text(
        text = stringResource(
            if (isFollowing) R.string.public_project_following else R.string.public_project_follow,
        ),
        style = AppTypography.caption.copy(color = foreground, fontWeight = FontWeight.Bold),
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(
                if (isFollowing) Modifier.border(1.dp, AppColors.Border, shape) else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AppSpacing.m, vertical = AppSpacing.xs),
    )
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
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                rowItems.forEach { item ->
                    PublicProjectRelatedCard(
                        item = item,
                        onClick = { onRelatedClick(item.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
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
            .border(1.dp, AppColors.Border, shape)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(RelatedPreviewHeight)
                .background(PreviewGradient),
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

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14, heightDp = 960)
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

@Preview(showBackground = true, backgroundColor = 0xFF0F0F14, heightDp = 960)
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
