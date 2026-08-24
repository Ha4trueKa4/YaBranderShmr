package com.yandex.avatargenerator.feature.project.fake

import com.yandex.avatargenerator.feature.project.domain.ProjectExporter
import com.yandex.avatargenerator.feature.project.domain.model.ExportFormat

/**
 * Экспортёр-подделка: файл никуда не пишется, запоминаются аргументы вызова.
 *
 * @param result чем закончится выгрузка — успехом или сбоем.
 */
internal class FakeProjectExporter(
    var result: Result<Unit> = Result.success(Unit),
) : ProjectExporter {

    var callCount: Int = 0
        private set
    var lastFormat: ExportFormat? = null
        private set
    var lastSvgUrl: String? = null
        private set

    override suspend fun export(
        svgUrl: String,
        baseName: String,
        format: ExportFormat,
    ): Result<Unit> {
        callCount++
        lastSvgUrl = svgUrl
        lastFormat = format
        return result
    }
}
