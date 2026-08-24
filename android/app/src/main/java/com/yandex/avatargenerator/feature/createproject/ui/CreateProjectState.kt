package com.yandex.avatargenerator.feature.createproject.ui

import com.yandex.avatargenerator.core.mvi.BaseState
import com.yandex.avatargenerator.feature.project.ui.ModelPickerState

/**
 * Состояние формы создания проекта.
 *
 * Здесь это одно data-состояние с флагами, а не набор взаимоисключающих экранов:
 * поля формы меняются независимо и сосуществуют.
 *
 * @property prompt описание того, что сгенерировать, — единственное поле,
 *  которое требует бэкенд.
 * @property name название проекта; необязательно.
 * @property description описание/бриф бренда; необязательно.
 * @property isSubmitting идёт создание проекта (генерация) — блокирует кнопку.
 * @property hasError последняя попытка генерации сорвалась; экран показывает
 *  сообщение об ошибке, кнопка снова активна для повтора.
 * @property modelPicker выбор модели генерации — тот же вложенный кусок
 *  состояния, что у экрана проекта: пикер один и тот же, и первичная генерация
 *  уходит на выбранной здесь модели.
 */
internal data class CreateProjectState(
    val prompt: String = "",
    val name: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val hasError: Boolean = false,
    val modelPicker: ModelPickerState = ModelPickerState(),
) : BaseState {

    /**
     * Кнопка активна, только когда есть непустой промпт и не идёт создание.
     * Гейтим именно по промпту, а не по названию: имя бэкенду больше не
     * требуется, и требовать его — лишний барьер перед единственным действием
     * экрана.
     */
    val canSubmit: Boolean get() = prompt.isNotBlank() && !isSubmitting
}
