package com.yandex.avatargenerator.feature.project.domain.model

import java.time.Instant

/**
 * Сообщение в ленте проекта.
 *
 * Именно `sealed interface`, а не data-класс с флагом `isOutgoing`: виды
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
     * Удачный ответ ИИ: пузырь у левого края с превью результата.
     *
     * Своего текста у такого ответа нет — в истории лежит только ссылка на
     * версию, подпись пузыря UI берёт из `strings.xml` (`domain/` про ресурсы
     * не знает).
     *
     * @property versionId версия, которую создала генерация. По ней UI ходит за
     *  ссылкой на превью и по ней же кэширует картинку: presigned-ссылка живёт
     *  час и при каждом запросе новая, так что ключом кэша быть не может.
     * @property previewUrl готовая ссылка на SVG версии; `null` — ссылку ещё
     *  предстоит запросить (в истории её не отдают).
     * @property jobId задача, которая создала этот ответ; `null` — записи
     *  старого формата, у них `job_id` нет. По нему экран узнаёт свой ответ в
     *  перечитанной ленте (см. `ProjectViewModel.reloadUntilAnswered`).
     */
    data class Incoming(
        override val id: String,
        val versionId: String,
        val sentAt: Instant?,
        val previewUrl: String? = null,
        val jobId: String? = null,
    ) : ChatMessage

    /**
     * Генерация идёт: скелетон на месте будущего ответа ИИ. Ждать можно
     * минутами, поэтому одного спиннера в кнопке отправки мало.
     *
     * Бывает двух происхождений, и это важно. Свой, локальный — экран ставит
     * его сразу после отправки промпта. **Серверный** — история чата отдаёт
     * заглушку активной задачи (`status = pending`), которой в БД нет; по ней
     * скелетон возвращается на место после перезахода в чат, хотя опрос статуса
     * к тому моменту давно оборван вместе с прошлым `viewModelScope`.
     *
     * @property jobId задача, которую ждём. По нему экран узнаёт незакрытую
     *  генерацию в перечитанной ленте, не дублирует скелетон своим и знает,
     *  что именно останавливать (`ProjectUiState.Content.pendingJobId`).
     */
    data class Pending(
        override val id: String,
        val jobId: String,
    ) : ChatMessage

    /**
     * Провалившийся ответ ИИ: пузырь ошибки с кнопкой «Повторить».
     *
     * Ровно то же лежит в истории на сервере (`role = assistant`, поле `error`),
     * поэтому экран выглядит одинаково сразу после провала и после перезахода —
     * снекбар такого свойства не даёт.
     *
     * @property text текст ошибки **с сервера**, показываем как есть; `null` —
     *  текста не прислали (или ошибка клиентская), подпись возьмёт UI из
     *  `strings.xml`.
     * @property jobId задача, которая упала; `null` — пузырь нарисован
     *  локально, на сервере такой записи нет. См. [Incoming.jobId].
     */
    data class Failed(
        override val id: String,
        val text: String?,
        val sentAt: Instant?,
        val jobId: String? = null,
    ) : ChatMessage

    /**
     * Прерванная генерация: пилюля «генерация отменена» на месте ответа ИИ.
     *
     * Такая же запись лежит в истории на сервере (`role = assistant`,
     * `status = cancelled`), так что после перезахода она остаётся на месте —
     * в отличие от [System.GenerationStillRunning], который живёт только на
     * экране. Своего текста у неё нет, как и у [Incoming]: подпись возьмёт UI
     * из `strings.xml`.
     *
     * @property jobId задача, которую прервали; `null` — записи старого
     *  формата. См. [Incoming.jobId].
     */
    data class Cancelled(
        override val id: String,
        val sentAt: Instant?,
        val jobId: String? = null,
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
         * @property version номер **по порядку удачных ответов в ленте**, а не
         *  `version_num` из базы: версию 1 бэкенд заводит сам при создании
         *  проекта и в историю чата не пишет, поэтому первый же ответ получил бы
         *  разделитель «Version 2 saved» — со стороны это выглядит как баг.
         *  Провалившиеся ответы номер не сдвигают: версии они не создают.
         * @property versionId та же версия, что и у [Incoming] выше: нужен как
         *  ключ кэша картинки.
         */
        data class VersionSaved(
            override val id: String,
            val version: String,
            val versionId: String,
            val createdAt: Instant?,
        ) : System

        /**
         * Пилюля «генерация ещё идёт, загляните позже» — вышел дедлайн ожидания
         * ([GenerationOutcome.TimedOut]). Локальная, на сервере записи нет.
         */
        data class GenerationStillRunning(override val id: String) : System
    }
}
