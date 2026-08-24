package com.yandex.avatargenerator.feature.createproject.ui

import com.yandex.avatargenerator.core.mvi.BaseEffect

/**
 * Одноразовые события экрана создания проекта — вся навигация отсюда. Отдаются
 * через канал, а не через [CreateProjectState], чтобы переход не повторялся на
 * каждой рекомпозиции.
 */
internal sealed interface CreateProjectEffect : BaseEffect {

    /** Проект создан; открыть его экран по [projectId]. */
    data class NavigateToProject(val projectId: String) : CreateProjectEffect

    data object NavigateBack : CreateProjectEffect
}
