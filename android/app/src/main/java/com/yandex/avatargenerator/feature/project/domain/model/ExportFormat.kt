package com.yandex.avatargenerator.feature.project.domain.model

/**
 * Формат выгрузки превью проекта. Проект хранится как SVG, поэтому SVG отдаётся
 * как есть, а PNG растрируется из него. [extension]/[mimeType] нужны слою data
 * для имени файла и записи в хранилище.
 *
 * У SVG MIME намеренно НЕ `image/svg+xml`: любой MIME семейства image MediaStore
 * помечает как изображение (`media_type=image`), и галерея тащит SVG к себе,
 * показывая пустой thumbnail (рисовать вектор она не умеет). `text/xml` держит
 * SVG-файл документом — он остаётся в «Загрузках» и не засоряет галерею.
 */
enum class ExportFormat(val extension: String, val mimeType: String) {
    PNG("png", "image/png"),
    SVG("svg", "text/xml"),
}
