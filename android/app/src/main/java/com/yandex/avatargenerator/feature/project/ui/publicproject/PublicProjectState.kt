package com.yandex.avatargenerator.feature.project.ui.publicproject

import com.yandex.avatargenerator.core.mvi.BaseState
import com.yandex.avatargenerator.feature.project.domain.model.PublicProject

/**
 * Состояние экрана публичного проекта: один immutable-класс со всеми флагами,
 * как требует [BaseState]. Раньше здесь была взаимоисключающая sealed-иерархия
 * `Loading/Content/Error`.
 *
 * @property project загруженный проект; `null`, пока его нет (загрузка или ошибка).
 * @property isExporting идёт выгрузка превью — повторные тапы игнорируем.
 *  Раньше это был приватный `exportJob`, невидимый ни UI, ни тестам.
 * @property isFollowInFlight запрос подписки в полёте — тоже бывший `followJob`.
 */
internal data class PublicProjectState(
    val isLoading: Boolean = true,
    val project: PublicProject? = null,
    val hasError: Boolean = false,
    val isExporting: Boolean = false,
    val isFollowInFlight: Boolean = false,
) : BaseState
