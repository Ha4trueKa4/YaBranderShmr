package com.yandex.avatargenerator.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Политика повторов из [installApiRetry] — ровно та, что уходит в прод. */
class ApiRetryTest {

    private val attempts = AtomicInteger(0)

    private fun clientAlwaysFailing(status: HttpStatusCode): HttpClient {
        val engine = MockEngine {
            attempts.incrementAndGet()
            respondError(status)
        }
        return HttpClient(engine) {
            expectSuccess = true
            installApiRetry()
        }
    }

    @Test
    fun `GET с 5xx повторяется`() = runTest {
        val client = clientAlwaysFailing(HttpStatusCode.BadGateway)

        runCatching { client.get("payload") }

        assertEquals(3, attempts.get()) // исходный запрос + 2 повтора
    }

    @Test
    fun `POST с 5xx не повторяется`() = runTest {
        val client = clientAlwaysFailing(HttpStatusCode.BadGateway)

        runCatching { client.post("generate") }

        assertEquals(1, attempts.get())
    }

    @Test
    fun `GET с 4xx не повторяется`() = runTest {
        val client = clientAlwaysFailing(HttpStatusCode.BadRequest)

        runCatching { client.get("payload") }

        assertEquals(1, attempts.get())
    }

    @Test
    fun `успешный запрос выполняется один раз`() = runTest {
        val engine = MockEngine {
            attempts.incrementAndGet()
            respondOk()
        }
        val client = HttpClient(engine) {
            expectSuccess = true
            installApiRetry()
        }

        client.get("payload")

        assertEquals(1, attempts.get())
    }
}
