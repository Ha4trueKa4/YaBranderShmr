package com.yandex.avatargenerator.feature.project.ui

import com.yandex.avatargenerator.feature.project.domain.model.GenerationModel

/**
 * Состояние пикера модели генерации — вложенным куском, а не россыпью полей в
 * [ProjectUiState.Content]: при переезде экрана на MVI переедет одно поле, а
 * содержимое пикера останется нетронутым.
 *
 * @property selectedModelId выбранная модель. Ровно то, что уходит в теле
 *  `POST /generate`; чип рисует её имя без похода в сеть.
 * @property models справочник. Пуст, пока шторку не открывали: список тянем
 *  лениво, при первом открытии.
 * @property isLoading идёт загрузка справочника.
 * @property hasError справочник не прочитался либо пришёл пустым. Ошибка живёт
 *  внутри шторки и отправку промптов не блокирует: в теле уедет сохранённый
 *  `id`, а не найдётся такой на сервере — ответит сам сервер.
 */
internal data class ModelPickerState(
    val selectedModelId: String = GenerationModel.DefaultId,
    val models: List<GenerationModel> = emptyList(),
    val isSheetVisible: Boolean = false,
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
)
