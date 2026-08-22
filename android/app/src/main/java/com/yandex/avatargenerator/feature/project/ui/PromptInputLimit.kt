package com.yandex.avatargenerator.feature.project.ui

/**
 * Потолок промпта — **1500 байт UTF-8, а не символов**: бэкенд меряет
 * `prompt.size()` в C++, то есть длину байтовой строки. Кириллица занимает по
 * два байта, поэтому по-русски влезает около 750 символов, а по-английски —
 * все 1500.
 *
 * В отличие от [com.yandex.avatargenerator.feature.auth.ui.MaxAuthInputLength]
 * это не «защита от абсурда», а ровно то же ограничение, что стоит на сервере:
 * дублируем его, чтобы отправка не возвращалась с 400.
 */
internal const val MaxPromptBytes = 1500

/**
 * Обрезает текст так, чтобы в UTF-8 он занимал не больше [maxBytes] байт.
 *
 * Режем по границе кодовой точки, а не байта: иначе последний символ превратится
 * в «половинку» и уедет на сервер битым.
 */
internal fun limitPromptBytes(text: String, maxBytes: Int = MaxPromptBytes): String {
    if (text.toByteArray().size <= maxBytes) return text

    var bytes = 0
    var index = 0
    while (index < text.length) {
        val codePoint = text.codePointAt(index)
        val size = utf8Size(codePoint)
        if (bytes + size > maxBytes) break
        bytes += size
        index += Character.charCount(codePoint)
    }
    return text.substring(0, index)
}

private fun utf8Size(codePoint: Int): Int = when {
    codePoint < 0x80 -> 1
    codePoint < 0x800 -> 2
    codePoint < 0x10000 -> 3
    else -> 4
}
