package com.yandex.avatargenerator.feature.project.ui

import com.yandex.avatargenerator.feature.project.domain.model.PublicProject

/**
 * Состояние экрана публичного проекта.
 *
 * `sealed interface`, а не data-класс с флагами: состояния взаимоисключающие, и
 * полноту `when` по ним проверяет компилятор (ср. [ProjectUiState]).
 */
internal sealed interface PublicProjectUiState {

    /** Первичная загрузка проекта. */
    data object Loading : PublicProjectUiState

    /** Проект загружен; экран рисуется целиком из [project]. */
    data class Content(val project: PublicProject) : PublicProjectUiState

    /** Загрузка не удалась; пользователю доступен повтор. */
    data object Error : PublicProjectUiState
}
