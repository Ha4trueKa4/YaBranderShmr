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
     * @property draft текст в поле ввода. Живёт в состоянии ViewModel, а не в
     *  `remember` экрана: так переживает поворот без `rememberSaveable` и
     *  покрывается юнит-тестом.
     */
    data class Content(
        val title: String,
        val messages: List<ChatMessage>,
        val draft: String = "",
    ) : ProjectUiState

    /** Загрузка не удалась; пользователю доступен повтор. */
    data object Error : ProjectUiState
}
