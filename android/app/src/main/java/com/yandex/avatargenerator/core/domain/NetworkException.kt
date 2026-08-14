package com.yandex.avatargenerator.core.domain

/**
 * Единая доменная ошибка сети: репозитории бросают только её, поэтому
 * `domain/` и `ui/` не знают ни про Ktor, ни про HTTP-коды.
 *
 * `cause` сохраняем всегда — иначе в крэш-репорте от «HTTP 400» не останется
 * ни адреса, ни метода запроса.
 */
sealed class NetworkException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** Нет интернета / хост недоступен. */
    class NoConnection(cause: Throwable? = null) : NetworkException("No connection", cause)

    /** Сервер не ответил за отведённое время. */
    class Timeout(cause: Throwable? = null) : NetworkException("Request timeout", cause)

    /** 401 — токена нет, он протух или отозван. */
    class Unauthorized(
        val serverMessage: String? = null,
        cause: Throwable? = null,
    ) : NetworkException(serverMessage, cause)

    /** 403 — токен валиден, но прав на операцию нет. */
    class Forbidden(
        val serverMessage: String? = null,
        cause: Throwable? = null,
    ) : NetworkException(serverMessage, cause)

    /** 404 — ресурса не существует. */
    class NotFound(
        val serverMessage: String? = null,
        cause: Throwable? = null,
    ) : NetworkException(serverMessage, cause)

    /** Прочие 4xx: запрос неверен или отклонён валидацией. */
    class ClientError(
        val code: Int,
        val serverMessage: String? = null,
        cause: Throwable? = null,
    ) : NetworkException("HTTP $code: $serverMessage", cause)

    /** 5xx: проблема на стороне бэкенда. */
    class ServerError(
        val code: Int,
        val serverMessage: String? = null,
        cause: Throwable? = null,
    ) : NetworkException("HTTP $code: $serverMessage", cause)

    /** Ответ пришёл, но не разобрался — контракт разошёлся с бэкендом. */
    class Serialization(cause: Throwable? = null) : NetworkException("Malformed response", cause)

    /** Всё, что не удалось классифицировать. */
    class Unknown(cause: Throwable? = null) : NetworkException(cause?.message, cause)
}
