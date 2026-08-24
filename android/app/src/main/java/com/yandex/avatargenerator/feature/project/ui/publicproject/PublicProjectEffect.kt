package com.yandex.avatargenerator.feature.project.ui.publicproject

import com.yandex.avatargenerator.core.mvi.BaseEffect
import com.yandex.avatargenerator.feature.project.domain.model.ExportFormat

/**
 * Одноразовые события экрана публичного проекта: навигация и результат выгрузки
 * для тоста. В [PublicProjectState] такому не место — показали и забыли, иначе
 * тост всплывал бы заново на каждой рекомпозиции.
 */
internal sealed interface PublicProjectEffect : BaseEffect {

    data object NavigateBack : PublicProjectEffect

    data class OpenAuthor(val authorId: String) : PublicProjectEffect

    data class OpenRelated(val projectId: String) : PublicProjectEffect

    /** Файл сохранён в «Загрузки» (SVG) или галерею (PNG). */
    data class ShowExportSuccess(val format: ExportFormat) : PublicProjectEffect

    /** Не удалось сохранить (сеть, рендер, запись). */
    data class ShowExportFailure(val format: ExportFormat) : PublicProjectEffect

    /** У проекта ещё нет превью — выгружать нечего. */
    data object ShowNothingToExport : PublicProjectEffect
}
