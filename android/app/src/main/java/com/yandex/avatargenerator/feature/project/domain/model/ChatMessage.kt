package com.yandex.avatargenerator.feature.project.domain.model

import java.time.Instant

/**
 * Сообщение в ленте проекта.
 *
 * Именно `sealed interface`, а не data-класс с флагом `isOutgoing`: три вида
 * сообщений рисуются принципиально по-разному, и полноту `when` по ним проверяет
 * компилятор — нельзя случайно забыть ветку (тот же приём, что в `ProjectUiState`).
 */
sealed interface ChatMessage {

    val id: String

    /**
     * Сообщение пользователя: пузырь у правого края.
     *
     * @property sentAt время из истории; `null`, если бэкенд его не прислал или
     *  прислал в неразобранном формате — подпись под пузырём просто не рисуется.
     */
    data class Outgoing(
        override val id: String,
        val text: String,
        val sentAt: Instant?,
    ) : ChatMessage

    /**
     * Ответ ИИ: пузырь у левого края с превью результата.
     *
     * Своего текста у ответа нет — в истории лежит только ссылка на версию,
     * подпись пузыря UI берёт из `strings.xml` (`domain/` про ресурсы не знает).
     *
     * @property versionId версия, которую создала генерация. По ней UI ходит за
     *  ссылкой на превью и по ней же кэширует картинку: presigned-ссылка живёт
     *  час и при каждом запросе новая, так что ключом кэша быть не может.
     * @property previewUrl готовая ссылка на SVG версии; `null` — ссылку ещё
     *  предстоит запросить (в истории её не отдают).
     */
    data class Incoming(
        override val id: String,
        val versionId: String,
        val sentAt: Instant?,
        val previewUrl: String? = null,
    ) : ChatMessage

    /**
     * Системные события ленты: своего текста не несут — UI подставит строку
     * из `strings.xml` по типу события.
     *
     * Почему без поля `text`: `domain/` по `ARCHITECTURE.md` не знает про Android,
     * а тексты должны жить в ресурсах. Данные (номер версии) в модели остаются.
     */
    sealed interface System : ChatMessage {

        /** Пилюля «Project created successfully» в начале ленты. */
        data class ProjectCreated(override val id: String) : System

        /**
         * Разделитель «VERSION 2 SAVED» под ответом ИИ.
         *
         * @property version номер **по порядку ответов в ленте**, а не
         *  `version_num` из базы: версию 1 бэкенд заводит сам при создании
         *  проекта и в историю чата не пишет, поэтому первый же ответ получил бы
         *  разделитель «Version 2 saved» — со стороны это выглядит как баг.
         * @property versionId та же версия, что и у [Incoming] выше: нужен как
         *  ключ кэша картинки.
         */
        data class VersionSaved(
            override val id: String,
            val version: String,
            val versionId: String,
            val createdAt: Instant?,
        ) : System
    }
}
