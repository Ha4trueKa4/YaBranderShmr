package com.yandex.avatargenerator.core.network

import com.yandex.avatargenerator.BuildConfig

/** Параметры подключения к API. */
object ApiConfig {

    /** Завершающий слэш обязателен — иначе Ktor затрёт последний сегмент пути. */
    val BaseUrl: String = BuildConfig.API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }

    const val RequestTimeoutMillis: Long = 30_000
    const val ConnectTimeoutMillis: Long = 15_000
    const val SocketTimeoutMillis: Long = 30_000

    /**
     * Отдельный таймаут для `POST /projects/{id}/generate`: LLM думает дольше
     * общих 30 секунд. Ставится на самом запросе через `timeout { }` — поднимать
     * ради него потолок всему клиенту нельзя.
     *
     * Значение синхронизировано с бэкендом: там ожидание ответа модели тоже 240
     * секунд (`request.timeout(240000)` в `LLMClientComponent.cpp`). Оборвать
     * раньше — значит потерять уже сгенерированную версию: бэкенд доводит
     * генерацию до конца и дописывает её в историю чата независимо от клиента.
     */
    const val GenerationTimeoutMillis: Long = 240_000

    /**
     * Сколько ждать версию опросом, если запрос генерации всё-таки оборвался
     * (мобильная сеть рвёт сокет и раньше [GenerationTimeoutMillis]). Бэкенд к
     * этому моменту чаще всего ещё считает — версия появляется чуть позже.
     */
    const val GenerationPollWindowMillis: Long = 60_000

    /** Пауза между опросами внутри [GenerationPollWindowMillis]. */
    const val GenerationPollIntervalMillis: Long = 10_000
}
