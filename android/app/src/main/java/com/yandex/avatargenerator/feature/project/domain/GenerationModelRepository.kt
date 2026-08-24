package com.yandex.avatargenerator.feature.project.domain

import com.yandex.avatargenerator.feature.project.domain.model.GenerationModel

/**
 * Модель генерации: справочник доступных и выбор пользователя.
 *
 * Сеть и настройка в одном репозитории намеренно — это одно понятие «модель
 * генерации», как у [com.yandex.avatargenerator.feature.settings.domain.SettingsRepository],
 * который держит и тему, и язык.
 *
 * Выбор глобальный, один на приложение: бэкенд его не хранит, а на экране
 * проекта он и так один.
 */
interface GenerationModelRepository {

    /**
     * Справочник моделей (`GET /models`, авторизация не нужна).
     *
     * Список короткий (0–4 записи) и статический: бэкенд фильтрует ответ Yandex
     * AI Studio своим allow-list. **Пустой список — валидный ответ**, экран
     * трактует его как ошибку шторки.
     *
     * @return модели с уже срезанным префиксом. `Result.failure` с
     *  [com.yandex.avatargenerator.core.domain.NetworkException] — справочник не
     *  прочитался; отправку промптов это не блокирует.
     */
    suspend fun getAvailableModels(): Result<List<GenerationModel>>

    /**
     * Выбранная модель или [GenerationModel.DefaultId], если выбора ещё не было.
     *
     * `suspend`, а не `Flow`: значение меняется только с экрана чата и читается
     * один раз при его открытии.
     */
    suspend fun getSelectedModelId(): String

    /** Запоминает выбор [id] — глобально, до следующего выбора. */
    suspend fun selectModel(id: String)
}
