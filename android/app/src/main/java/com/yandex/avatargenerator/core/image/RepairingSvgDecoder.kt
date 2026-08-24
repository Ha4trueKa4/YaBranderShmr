package com.yandex.avatargenerator.core.image

import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.svg.SvgDecoder
import coil3.svg.isSvg
import okio.Buffer

private const val SvgMimeType = "image/svg+xml"

/**
 * [SvgDecoder] со второй попыткой: если androidsvg не разобрал документ, разметку
 * прогоняем через [repairSvg] и пробуем ещё раз.
 *
 * Ставится вместо `SvgDecoder.Factory()` (см. `AvatarGeneratorApplication`) и
 * нужен из-за битых SVG от генератора: одно слово вместо числа обрывает разбор
 * всего файла, и вместо превью проекта везде (лента, профиль, список чатов)
 * остаётся серая плашка. Перегенерировать уже лежащие в S3 файлы клиент не может,
 * поэтому чинит их на лету.
 *
 * Починка запускается только после провала — у валидных превью путь прежний,
 * лишней работы на них нет. Если и починенный документ не разобрался, наружу
 * летит исходная ошибка: подменять причину диагностикой было бы враньём.
 */
class RepairingSvgDecoder(
    private val source: ImageSource,
    private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        // Байты держим у себя: исходный источник одноразовый, а на вторую
        // попытку разметка нужна ещё раз.
        val markup = source.use { it.source().readByteArray() }
        return try {
            decodeSvg(markup.decodeToString())
        } catch (parseFailure: Exception) {
            runCatching { decodeSvg(repairSvg(markup.decodeToString())) }
                .getOrElse { throw parseFailure }
        }
    }

    private suspend fun decodeSvg(markup: String): DecodeResult {
        val buffer = Buffer().writeUtf8(markup)
        return SvgDecoder(
            source = ImageSource(buffer, options.fileSystem),
            options = options,
        ).decode()
    }

    class Factory : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            if (!isApplicable(result)) return null
            return RepairingSvgDecoder(result.source, options)
        }

        // Тот же критерий, что у SvgDecoder.Factory: тип из ответа, а если его
        // нет — заглядываем в первые байты.
        private fun isApplicable(result: SourceFetchResult): Boolean =
            result.mimeType == SvgMimeType || DecodeUtils.isSvg(result.source.source())
    }
}
