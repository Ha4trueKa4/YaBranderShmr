package com.yandex.avatargenerator.feature.project.ui

import com.yandex.avatargenerator.feature.project.domain.model.ExportFormat

/**
 * Одноразовый результат выгрузки превью для тоста на экране публичного проекта.
 * Не часть [PublicProjectUiState] — это событие, а не состояние: показали и забыли.
 */
internal sealed interface ExportEvent {

    /** Файл сохранён в «Загрузки». */
    data class Success(val format: ExportFormat) : ExportEvent

    /** Не удалось сохранить (сеть, рендер, запись). */
    data class Failure(val format: ExportFormat) : ExportEvent

    /** У проекта ещё нет превью — выгружать нечего. */
    data object NothingToExport : ExportEvent
}
