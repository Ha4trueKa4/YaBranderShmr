package com.yandex.avatargenerator.feature.project.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.yandex.avatargenerator.core.network.apiCall
import com.yandex.avatargenerator.core.network.apiResult
import com.yandex.avatargenerator.feature.project.domain.ProjectExporter
import com.yandex.avatargenerator.feature.project.domain.model.ExportFormat
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

/**
 * Растеризует/качает превью проекта и сохраняет его на устройство.
 *
 * SVG берём как есть по [svgUrl] (Ktor-клиент подставляет `Bearer` сам).
 * PNG растрируем из того же SVG через singleton-[coil3.ImageLoader] приложения —
 * у него уже настроен `SvgDecoder`, поэтому отдельный рендерер не нужен.
 *
 * Куда кладём: **PNG — в галерею** (`MediaStore.Images`, папка Pictures), чтобы он
 * был виден в «Фото»; **SVG — в «Загрузки»** (галерея векторную графику не
 * показывает). Всё без разрешений: на Android 10+ (API 29) — через MediaStore;
 * на API 26–28 — в app-specific папку приложения (+ media-scan для картинки, чтобы
 * её подхватила галерея), чтобы не тянуть runtime-`WRITE_EXTERNAL_STORAGE`.
 */
class AndroidProjectExporter(
    private val context: Context,
    private val client: HttpClient,
) : ProjectExporter {

    override suspend fun export(
        svgUrl: String,
        baseName: String,
        format: ExportFormat,
    ): Result<Unit> = apiResult {
        val fileName = buildFileName(baseName, format)
        withContext(Dispatchers.IO) {
            when (format) {
                ExportFormat.SVG -> {
                    val bytes = apiCall { client.get(svgUrl).body<ByteArray>() }
                    save(fileName, format.mimeType, Destination.DOWNLOADS) { it.write(bytes) }
                }

                ExportFormat.PNG -> {
                    val bitmap = renderSvgToBitmap(svgUrl)
                    save(fileName, format.mimeType, Destination.GALLERY) { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
                    }
                }
            }
        }
    }

    private suspend fun renderSvgToBitmap(svgUrl: String): Bitmap {
        val request = ImageRequest.Builder(context)
            .data(svgUrl)
            .size(EXPORT_SIZE_PX)
            // Растр нужно сжать в PNG — hardware-bitmap для этого не читается.
            .allowHardware(false)
            .build()
        val result = SingletonImageLoader.get(context).execute(request)
        val image = (result as? SuccessResult)?.image
            ?: error("Failed to render SVG for export")
        return image.toBitmap()
    }

    /** Куда сохраняем файл: галерея (картинки) или «Загрузки» (прочее). */
    private enum class Destination { GALLERY, DOWNLOADS }

    private fun save(
        fileName: String,
        mimeType: String,
        destination: Destination,
        write: (OutputStream) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // MediaStore.Downloads — API 29+, поэтому ссылки на него только здесь.
            val (collection, relativePath) = when (destination) {
                Destination.GALLERY ->
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI to
                        "${Environment.DIRECTORY_PICTURES}/$APP_FOLDER"

                Destination.DOWNLOADS ->
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI to Environment.DIRECTORY_DOWNLOADS
            }
            val resolver = context.contentResolver
            val pending = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, pending)
                ?: error("MediaStore rejected the entry")
            // Обрыв записи (сеть/растеризация) или отмена корутины при уходе с экрана
            // оставили бы запись с IS_PENDING = 1 — невидимый файл, занимающий место,
            // пока система не подчистит pending (до 7 дней). Откатываем сами.
            try {
                resolver.openOutputStream(uri)?.use(write) ?: error("Cannot open output stream")
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null,
                )
            } catch (t: Throwable) {
                resolver.delete(uri, null, null)
                throw t
            }
        } else {
            // На API 26–28 публичные папки требуют WRITE_EXTERNAL_STORAGE. Чтобы не
            // тянуть runtime-разрешение, пишем в app-specific папку приложения; для
            // картинки дополнительно просим media-scan, чтобы её показала галерея.
            val legacyDir = when (destination) {
                Destination.GALLERY -> Environment.DIRECTORY_PICTURES
                Destination.DOWNLOADS -> Environment.DIRECTORY_DOWNLOADS
            }
            val dir = context.getExternalFilesDir(legacyDir)
                ?: error("External files dir unavailable")
            dir.mkdirs()
            val file = File(dir, fileName)
            // При исключении на записи остался бы обрезанный файл — подчищаем.
            try {
                file.outputStream().use(write)
            } catch (t: Throwable) {
                file.delete()
                throw t
            }
            if (destination == Destination.GALLERY) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf(mimeType),
                    null,
                )
            }
        }
    }

    /** Чистим название от символов, недопустимых в имени файла, и вешаем метку времени. */
    private fun buildFileName(baseName: String, format: ExportFormat): String {
        val safe = baseName.trim()
            // Буквы/цифры любого алфавита (в т.ч. кириллица) валидны в имени файла —
            // режем только разделители пути и прочие спецсимволы.
            .replace(Regex("[^\\p{L}\\p{N}._-]+"), "_")
            .trim('_')
            .ifEmpty { DEFAULT_BASE_NAME }
        return "${safe}_${System.currentTimeMillis()}.${format.extension}"
    }

    private companion object {
        const val EXPORT_SIZE_PX = 1024
        const val PNG_QUALITY = 100
        const val DEFAULT_BASE_NAME = "project"

        // Подпапка в Pictures/, чтобы выгрузки приложения лежали отдельным альбомом.
        const val APP_FOLDER = "AvatarGenerator"
    }
}
