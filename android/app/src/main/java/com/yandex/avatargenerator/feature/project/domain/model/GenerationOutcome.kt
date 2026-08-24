package com.yandex.avatargenerator.feature.project.domain.model

/**
 * Чем закончилось ожидание фоновой генерации.
 *
 * Все варианты — это **успешно доведённое до конца ожидание**, поэтому они
 * едут в `Result.success`. `Result.failure` у
 * [com.yandex.avatargenerator.feature.project.domain.ChatRepository.awaitGeneration]
 * остаётся за случаями, когда опрос сорвался сам (403, неразобранный ответ) и о
 * судьбе генерации ничего не известно.
 */
sealed interface GenerationOutcome {

    /**
     * Версия готова.
     *
     * @property previewUrl presigned-ссылка на SVG новой версии, живёт час;
     *  `null` — сервер её не прислал. Экран всё равно перечитывает ленту (в
     *  ответе нет `version_id`, а без него ссылку некуда положить и нечем
     *  кэшировать), так что картинку он получает оттуда.
     */
    data class Done(val previewUrl: String?) : GenerationOutcome

    /**
     * Генерация упала. Такая же запись уже лежит в истории на сервере
     * (`role = assistant`, поле `error`), поэтому экран после провала и после
     * перезахода выглядит одинаково.
     *
     * @property message текст с сервера, показываем как есть; `null` — текста
     *  не прислали, подпись возьмёт UI из `strings.xml`.
     */
    data class Failed(val message: String?) : GenerationOutcome

    /**
     * Генерацию прервали через `POST .../generate/{job_id}/cancel`. Как и
     * провал, это исход, о котором знает сервер: в истории лежит запись
     * `role = assistant` со `status = cancelled`, поэтому экран после отмены и
     * после перезахода выглядит одинаково. Версии такая генерация не создаёт.
     */
    data object Cancelled : GenerationOutcome

    /**
     * Вышел общий дедлайн опроса. Это **не** провал генерации: задача на сервере
     * могла продолжаться и досчитаться уже без нас — результат появится в ленте
     * при следующем открытии экрана.
     */
    data object TimedOut : GenerationOutcome
}
