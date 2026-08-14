package com.yandex.avatargenerator.core.network

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.PortUnreachableException
import java.net.UnknownHostException

/** Истёк любой из таймаутов [ApiConfig]. */
internal fun Throwable.isTimeout(): Boolean =
    this is HttpRequestTimeoutException ||
        this is ConnectTimeoutException ||
        this is SocketTimeoutException

/**
 * Соединение не удалось установить. Проверяем конкретные типы, а не
 * `IOException` целиком: под него попадают ошибки TLS и обрыв чтения тела,
 * а это не «нет интернета» и повторять их вслепую нельзя.
 */
internal fun Throwable.isConnectionFailure(): Boolean =
    this is UnknownHostException ||
        this is NoRouteToHostException ||
        this is PortUnreachableException ||
        this is ConnectException
