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
     * Таймаут одного опроса `GET /projects/{id}/generate/{job_id}`: ручка
     * работает по long polling и держит соединение до 25 секунд, прежде чем
     * ответить `pending`. Общих 30 секунд формально хватает, но запас на сеть
     * получается пять секунд — оборванный по таймауту опрос стоил бы лишнего
     * круга. Ставится на самом запросе через `timeout { }`; сокетный поднимаем
     * вместе с запросным, иначе ожидание первых байт оборвёт движок.
     */
    const val StatusPollTimeoutMillis: Long = 40_000

    /**
     * Пауза между опросами статуса. Обычно её съедает сам long polling; нужна
     * как предохранитель, чтобы не уйти в горячий цикл, если сервер вдруг начнёт
     * отвечать `pending` мгновенно.
     */
    const val StatusPollGuardMillis: Long = 1_000

    /**
     * Общий дедлайн ожидания генерации. Дальше экран говорит «ещё идёт,
     * загляните позже»: задача на сервере живёт своей жизнью и досчитается без
     * клиента, результат будет виден при следующем открытии чата.
     */
    const val GenerationDeadlineMillis: Long = 300_000
}
