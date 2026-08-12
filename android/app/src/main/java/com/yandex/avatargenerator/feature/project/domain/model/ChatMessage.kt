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

    /** Сообщение пользователя: пузырь у правого края. */
    data class Outgoing(
        override val id: String,
        val text: String,
        val sentAt: Instant,
    ) : ChatMessage

    /** Ответ ИИ: пузырь у левого края, опционально с превью результата. */
    data class Incoming(
        override val id: String,
        val text: String,
        val sentAt: Instant,
        val imageUrl: String? = null,
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

        /** Разделитель «VERSION 1.2 SAVED». */
        data class VersionSaved(override val id: String, val version: String) : System
    }
}
