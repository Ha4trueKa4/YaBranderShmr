package com.yandex.avatargenerator.feature.createproject.ui

import com.yandex.avatargenerator.core.mvi.BaseIntent

/** Всё, что пользователь может сделать на форме создания проекта. */
internal sealed interface CreateProjectIntent : BaseIntent {

    data class OnNameChange(val value: String) : CreateProjectIntent

    data class OnDescriptionChange(val value: String) : CreateProjectIntent

    data object OnSubmitClick : CreateProjectIntent

    data object OnBackClick : CreateProjectIntent
}
