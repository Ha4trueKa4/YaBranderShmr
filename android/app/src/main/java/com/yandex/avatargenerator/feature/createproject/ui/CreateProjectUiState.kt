package com.yandex.avatargenerator.feature.createproject.ui

/**
 * Состояние формы создания проекта.
 *
 * Здесь это data-класс, а не sealed-иерархия (в отличие от [ChatsUiState]):
 * поля формы меняются независимо и сосуществуют, взаимоисключающих экранов нет.
 *
 * @property name название проекта.
 * @property description описание/бриф бренда.
 * @property isSubmitting идёт создание проекта (генерация) — блокирует кнопку.
 * @property hasError последняя попытка генерации сорвалась; экран показывает
 *  сообщение об ошибке, кнопка снова активна для повтора.
 */
internal data class CreateProjectUiState(
    val name: String = "",
    val description: String = "",
    val isSubmitting: Boolean = false,
    val hasError: Boolean = false,
) {
    /** Кнопка активна, только когда есть непустое название и не идёт создание. */
    val canSubmit: Boolean get() = name.isNotBlank() && !isSubmitting
}

/**
 * Одноразовое событие экрана — навигация после успешного создания.
 * Отдаётся через канал, а не через [CreateProjectUiState], чтобы переход не
 * повторялся на каждой рекомпозиции.
 */
internal sealed interface CreateProjectEvent {

    /** Проект создан; открыть его экран по [projectId]. */
    data class ProjectCreated(val projectId: String) : CreateProjectEvent
}
