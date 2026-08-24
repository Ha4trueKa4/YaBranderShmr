package com.yandex.avatargenerator.feature.createproject.ui

/**
 * Потолок промпта формы создания проекта — **5000 символов, а не байт**:
 * `POST /projects` проверяет его через `Utf8Length`, то есть считает кодовые
 * точки, а не длину байтовой строки.
 *
 * Это **не** [com.yandex.avatargenerator.feature.project.ui.MaxPromptBytes]:
 * там 1500 **байт** и это ограничение другого хендлера —
 * `POST /projects/{id}/generate`, которым уходят правки в чате проекта. Оба
 * лимита «про промпт» и спутать их легко, а цена ошибки — 400 на отправке,
 * поэтому у каждого хендлера своя константа рядом со своим экраном.
 */
internal const val MaxCreatePromptLength = 5000

/**
 * Обрезает текст до [maxLength] кодовых точек — ровно так, как их считает
 * сервер.
 *
 * `String.take` здесь не подходит: он меряет UTF-16 `Char`-ы и на границе
 * разрубит суррогатную пару, так что эмодзи уедет на сервер половинкой.
 */
internal fun limitCreatePrompt(text: String, maxLength: Int = MaxCreatePromptLength): String {
    // Кодовых точек всегда не больше, чем Char-ов, — короткий текст режем без обхода.
    if (text.length <= maxLength) return text

    var count = 0
    var index = 0
    while (index < text.length && count < maxLength) {
        index += Character.charCount(text.codePointAt(index))
        count++
    }
    return text.substring(0, index)
}
