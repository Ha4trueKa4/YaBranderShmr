package com.yandex.avatargenerator.feature.project.ui.publicproject

import com.yandex.avatargenerator.core.mvi.BaseIntent
import com.yandex.avatargenerator.feature.project.domain.model.ExportFormat

/** Всё, что пользователь может сделать на экране публичного проекта. */
internal sealed interface PublicProjectIntent : BaseIntent {

    /** Повтор после ошибки. */
    data object OnRetryClick : PublicProjectIntent

    data object OnBackClick : PublicProjectIntent

    /** Тап по автору — открыть его профиль. */
    data object OnAuthorClick : PublicProjectIntent

    /** Тап по работе автора в секции «More from this creator». */
    data class OnRelatedClick(val projectId: String) : PublicProjectIntent

    data object OnFollowClick : PublicProjectIntent

    /** Сохранить превью в выбранном формате. */
    data class OnExportClick(val format: ExportFormat) : PublicProjectIntent
}
