package com.yandex.avatargenerator.feature.createproject.ui

import com.yandex.avatargenerator.core.mvi.BaseState

/**
 * Состояние формы создания проекта.
 *
 * Здесь это одно data-состояние с флагами, а не набор взаимоисключающих экранов:
 * поля формы меняются независимо и сосуществуют.
 *
 * @property name название проекта.
 * @property description описание/бриф бренда.
 * @property isSubmitting идёт создание проекта (генерация) — блокирует кнопку.
 * @property hasError последняя попытка генерации сорвалась; экран показывает
 *  сообщение об ошибке, кнопка снова активна для повтора.
 */
internal data class CreateProjectState(
    val name: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val hasError: Boolean = false,
) : BaseState {

    /** Кнопка активна, только когда есть непустое название и не идёт создание. */
    val canSubmit: Boolean get() = name.isNotBlank() && !isSubmitting
}
