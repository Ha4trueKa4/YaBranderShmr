package com.yandex.avatargenerator.feature.project.ui

import com.yandex.avatargenerator.feature.project.domain.model.ChatMessage

/**
 * Состояние экрана проекта (чата с ИИ).
 *
 * Именно `sealed interface`, а не data-класс с флагами: состояния
 * взаимоисключающие, и полноту `when` по ним проверяет компилятор.
 */
internal sealed interface ProjectUiState {

    /** Первичная загрузка диалога. */
    data object Loading : ProjectUiState

    /**
     * Диалог загружен.
     *
     * @property title название проекта — из `GET /projects/{id}`, а не из чата.
     * @property isPublic видимость проекта из того же ответа. Показывается чипом
     *  в шапке; тап открывает шторку выбора видимости, переключение идёт через
     *  [ProjectViewModel.setVisibility].
     * @property draft текст в поле ввода. Живёт в состоянии ViewModel, а не в
     *  `remember` экрана: так переживает поворот без `rememberSaveable` и
     *  покрывается юнит-тестом.
     * @property isSending идёт генерация по отправленному промпту — от старта
     *  задачи до её исхода (это минуты: сам `POST` отвечает сразу, ждём мы
     *  результат опросом статус-ручки). Пока флаг поднят, кнопка отправки и
     *  «Повторить» неактивны: повторное нажатие создало бы вторую фоновую
     *  задачу и вторую пару сообщений в истории.
     * @property isCancelling отмена запрошена, но задача ещё не остановилась.
     *  Флаг снимается вместе с [isSending] — исход отменённой генерации всё
     *  равно приносит идущий опрос статуса. Пока он поднят, кнопка остановки
     *  неактивна: второй `POST .../cancel` ничего не ускорит.
     * @property modelPicker выбор модели генерации — чип над полем ввода и
     *  шторка со справочником; см. [ModelPickerState].
     */
    data class Content(
        val title: String,
        val isPublic: Boolean,
        val messages: List<ChatMessage>,
        val draft: String = "",
        val isSending: Boolean = false,
        val isCancelling: Boolean = false,
        val modelPicker: ModelPickerState = ModelPickerState(),
    ) : ProjectUiState {

        /**
         * Задача, которую сейчас можно прервать: та, чей скелетон стоит в ленте.
         * `null` — генерации нет либо `POST /generate` ещё не ответил `job_id`,
         * и отменять пока нечего.
         *
         * Считается из ленты, а не хранится полем: скелетон и так появляется и
         * исчезает ровно на границах ожидания, и второй источник правды о том
         * же разъезжался бы с ним.
         */
        val pendingJobId: String?
            get() = messages.filterIsInstance<ChatMessage.Pending>().lastOrNull()?.jobId

        /** Остановка доступна: задача известна и отмену ещё не запрашивали. */
        val canCancel: Boolean
            get() = isSending && !isCancelling && pendingJobId != null
    }

    /** Загрузка не удалась; пользователю доступен повтор. */
    data object Error : ProjectUiState
}
