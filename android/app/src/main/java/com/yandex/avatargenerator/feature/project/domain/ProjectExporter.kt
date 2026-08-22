package com.yandex.avatargenerator.feature.project.domain

import com.yandex.avatargenerator.feature.project.domain.model.ExportFormat

/**
 * Сохраняет превью проекта в общее хранилище устройства (папка «Загрузки»).
 *
 * Интерфейс в `domain/`, платформенная реализация — в `data/`
 * ([com.yandex.avatargenerator.feature.project.data.AndroidProjectExporter]):
 * запись в MediaStore/файл, сеть и растеризация SVG наружу не торчат.
 *
 * Возвращает [Result] и не бросает (кроме отмены корутины): успех/ошибку
 * показывает ViewModel.
 */
interface ProjectExporter {

    /**
     * @param svgUrl ссылка на SVG проекта (S3); из неё берётся и SVG, и растр для PNG.
     * @param baseName имя файла без расширения (обычно название проекта); слой data
     *   сам чистит его от недопустимых символов и добавляет расширение.
     * @param format итоговый формат файла (см. [ExportFormat]).
     */
    suspend fun export(svgUrl: String, baseName: String, format: ExportFormat): Result<Unit>
}
