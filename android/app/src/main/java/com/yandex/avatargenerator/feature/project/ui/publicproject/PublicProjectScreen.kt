package com.yandex.avatargenerator.feature.project.ui.publicproject

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.avatargenerator.R
import com.yandex.avatargenerator.core.mvi.CollectEffects
import com.yandex.avatargenerator.feature.project.domain.model.ExportFormat
import com.yandex.avatargenerator.ui.components.BrandBackTopBar
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Экран публичного проекта: грузит проект по [projectId] через
 * [PublicProjectViewModel] и показывает загрузку, ошибку или сам проект.
 *
 * Точка входа — тап по проекту в ленте (`Destination.PublicProject`), поэтому
 * сверху [BrandBackTopBar]: на системную кнопку «назад» как на единственный путь
 * возврата полагаться нельзя. `projectId` известен только при открытии, так
 * что передаём его параметром ([parametersOf]), как в `ProjectScreen`.
 *
 * Навигация и тосты приходят эффектами [PublicProjectEffect]: тап уходит
 * интентом во ViewModel, и уже она решает, куда вести и что показать.
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    CollectEffects(viewModel.effect) { effect ->
        when (effect) {
            PublicProjectEffect.NavigateBack -> onBackClick()
            is PublicProjectEffect.OpenAuthor -> onAuthorClick(effect.authorId)
            is PublicProjectEffect.OpenRelated -> onRelatedClick(effect.projectId)
            // Тост, а не состояние: показали и забыли. Экран собирает эффекты
            // только пока он STARTED (см. CollectEffects), поэтому событие,
            // пришедшее в фоне, не всплывёт мимо переднего плана.
            is PublicProjectEffect.ShowExportSuccess,
            is PublicProjectEffect.ShowExportFailure,
            PublicProjectEffect.ShowNothingToExport,
            -> Toast.makeText(context, effect.exportMessage(context), Toast.LENGTH_SHORT).show()
        }
    }

    PublicProjectContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

/**
 * Текст тоста по результату выгрузки. Имя формата — из ресурсов, без литералов.
 * Навигационные эффекты сюда не попадают — их разбирает `when` выше.
 */
private fun PublicProjectEffect.exportMessage(context: Context): String = when (this) {
    is PublicProjectEffect.ShowExportSuccess -> {
        // PNG уходит в галерею, SVG — в «Загрузки»; сообщение подсказывает, где искать.
        val template = when (format) {
            ExportFormat.PNG -> R.string.public_project_download_saved_gallery
            ExportFormat.SVG -> R.string.public_project_download_saved
        }
        context.getString(template, context.getString(format.labelRes))
    }

    is PublicProjectEffect.ShowExportFailure -> context.getString(
        R.string.public_project_download_failed,
        context.getString(format.labelRes),
    )

    PublicProjectEffect.ShowNothingToExport ->
        context.getString(R.string.public_project_download_empty)

    PublicProjectEffect.NavigateBack,
    is PublicProjectEffect.OpenAuthor,
    is PublicProjectEffect.OpenRelated,
    -> error("Navigation effect has no toast message")
}

private val ExportFormat.labelRes: Int
    get() = when (this) {
        ExportFormat.PNG -> R.string.export_format_png
        ExportFormat.SVG -> R.string.export_format_svg
    }
