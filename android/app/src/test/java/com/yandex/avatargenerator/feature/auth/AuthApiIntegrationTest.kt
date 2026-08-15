package com.yandex.avatargenerator.feature.auth

import com.yandex.avatargenerator.core.network.ApiJson
import com.yandex.avatargenerator.core.network.InMemoryAuthTokenStore
import com.yandex.avatargenerator.core.network.installApiRetry
import com.yandex.avatargenerator.feature.auth.data.AuthRepositoryImpl
import com.yandex.avatargenerator.feature.auth.domain.AuthException
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Проверка контракта авторизации на живом стенде. По умолчанию пропускается —
 * тест ходит в сеть и создаёт пользователей, в CI такому не место.
 *
 * ```
 * API_INTEGRATION=1 ./gradlew :app:testDebugUnitTest --tests '*AuthApiIntegrationTest'
 * ```
 * Адрес стенда переопределяется переменной `API_BASE_URL`.
 */
class AuthApiIntegrationTest {

    private val baseUrl: String =
        System.getenv("API_BASE_URL") ?: "http://158.160.187.118:8080/api/v1/"

    /**
     * Клиент собираем вручную: [com.yandex.avatargenerator.core.network.HttpClientFactory]
     * тянет `BuildConfig` и `android.util.Log`, которых на JVM нет. Политику
     * повторов берём из общего кода, чтобы проверять ту же, что уходит в прод.
     */
    private fun repository(): Pair<AuthRepositoryImpl, InMemoryAuthTokenStore> {
        val tokenStore = InMemoryAuthTokenStore()
        val client = HttpClient(OkHttp.create()) {
            expectSuccess = true
            install(ContentNegotiation) { json(ApiJson) }
            install(HttpTimeout) { requestTimeoutMillis = 30_000 }
            installApiRetry()
            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }
        }
        // Профиль здесь не проверяем, поэтому хранилище пользователя — в памяти.
        return AuthRepositoryImpl(client, tokenStore, FakeAuthUserStore()) to tokenStore
    }

    /** Уникальный логин на прогон: повторная регистрация того же — уже 409. */
    private fun newCredentials(): Triple<String, String, String> {
        val suffix = System.nanoTime().toString().takeLast(12)
        return Triple("android.it.$suffix@example.com", "Passw0rd!23", "androidit$suffix")
    }

    @Test
    fun `регистрация возвращает токен и кладёт его в стор`() = runTest {
        assumeTrue(System.getenv("API_INTEGRATION") == "1")
        val (repository, tokenStore) = repository()
        val (email, password, nickname) = newCredentials()

        val session = repository.register(email, password, nickname)

        assertTrue(session.accessToken.isNotBlank())
        assertEquals("Bearer", session.tokenType)
        assertEquals(email, session.user.email)
        assertEquals(nickname, session.user.nickname)
        assertNotNull(session.user.id)
        assertEquals(session.accessToken, tokenStore.currentToken())
    }

    @Test
    fun `вход после регистрации отдаёт того же пользователя`() = runTest {
        assumeTrue(System.getenv("API_INTEGRATION") == "1")
        val (repository, _) = repository()
        val (email, password, nickname) = newCredentials()
        val registered = repository.register(email, password, nickname)

        val session = repository.login(email, password)

        assertEquals(registered.user.id, session.user.id)
        assertTrue(session.accessToken.isNotBlank())
    }

    @Test
    fun `повторная регистрация email даёт конфликт по полю email`() = runTest {
        assumeTrue(System.getenv("API_INTEGRATION") == "1")
        val (repository, _) = repository()
        val (email, password, nickname) = newCredentials()
        repository.register(email, password, nickname)

        val conflict = runCatching { repository.register(email, password, "${nickname}x") }
            .exceptionOrNull()

        assertTrue("получили $conflict", conflict is AuthException.RegistrationConflict)
        assertEquals(
            AuthException.Field.Email,
            (conflict as AuthException.RegistrationConflict).field,
        )
    }

    @Test
    fun `повторная регистрация никнейма даёт конфликт по полю nickname`() = runTest {
        assumeTrue(System.getenv("API_INTEGRATION") == "1")
        val (repository, _) = repository()
        val (email, password, nickname) = newCredentials()
        repository.register(email, password, nickname)

        val conflict = runCatching { repository.register("x$email", password, nickname) }
            .exceptionOrNull()

        assertTrue("получили $conflict", conflict is AuthException.RegistrationConflict)
        assertEquals(
            AuthException.Field.Nickname,
            (conflict as AuthException.RegistrationConflict).field,
        )
    }

    @Test
    fun `неверный пароль — InvalidCredentials, а не протухшая сессия`() = runTest {
        assumeTrue(System.getenv("API_INTEGRATION") == "1")
        val (repository, _) = repository()
        val (email, password, nickname) = newCredentials()
        repository.register(email, password, nickname)

        val error = runCatching { repository.login(email, "wrong-$password") }.exceptionOrNull()

        assertTrue("получили $error", error is AuthException.InvalidCredentials)
    }

    @Test
    fun `невалидный email отклоняется с привязкой к полю email`() = runTest {
        assumeTrue(System.getenv("API_INTEGRATION") == "1")
        val (repository, _) = repository()

        val error = runCatching { repository.register("not-an-email", "Passw0rd23", "nickname") }
            .exceptionOrNull()

        assertTrue("получили $error", error is AuthException.FieldRejected)
        assertEquals(AuthException.Field.Email, (error as AuthException.FieldRejected).field)
        assertNotNull(error.serverMessage)
    }

    @Test
    fun `короткий пароль отклоняется с привязкой к полю password`() = runTest {
        assumeTrue(System.getenv("API_INTEGRATION") == "1")
        val (repository, _) = repository()
        val (email, _, nickname) = newCredentials()

        val error = runCatching { repository.register(email, "Ab1", nickname) }.exceptionOrNull()

        assertTrue("получили $error", error is AuthException.FieldRejected)
        assertEquals(
            AuthException.Field.Password,
            (error as AuthException.FieldRejected).field,
        )
    }

    @Test
    fun `пароль без цифр отклоняется — правило проверяет сервер, не клиент`() = runTest {
        assumeTrue(System.getenv("API_INTEGRATION") == "1")
        val (repository, _) = repository()
        val (email, _, nickname) = newCredentials()

        val error = runCatching { repository.register(email, "PasswordOnly", nickname) }
            .exceptionOrNull()

        assertEquals(
            AuthException.Field.Password,
            (error as AuthException.FieldRejected).field,
        )
    }

    @Test
    fun `короткий никнейм отклоняется с привязкой к полю nickname`() = runTest {
        assumeTrue(System.getenv("API_INTEGRATION") == "1")
        val (repository, _) = repository()
        val (email, password, _) = newCredentials()

        val error = runCatching { repository.register(email, password, "ab") }.exceptionOrNull()

        assertTrue("получили $error", error is AuthException.FieldRejected)
        assertEquals(
            AuthException.Field.Nickname,
            (error as AuthException.FieldRejected).field,
        )
    }

    /** Без нормализации в репозитории этот вход упал бы с 401. */
    @Test
    fun `вход работает независимо от регистра и пробелов в email`() = runTest {
        assumeTrue(System.getenv("API_INTEGRATION") == "1")
        val (repository, _) = repository()
        val (email, password, nickname) = newCredentials()
        val registered = repository.register(email.uppercase(), password, nickname)

        val session = repository.login("  $email  ", password)

        assertEquals(registered.user.id, session.user.id)
    }

    @Test
    fun `регистрация тем же email в другом регистре — конфликт, а не второй аккаунт`() = runTest {
        assumeTrue(System.getenv("API_INTEGRATION") == "1")
        val (repository, _) = repository()
        val (email, password, nickname) = newCredentials()
        repository.register(email, password, nickname)

        val conflict = runCatching { repository.register(email.uppercase(), password, "${nickname}x") }
            .exceptionOrNull()

        assertTrue("получили $conflict", conflict is AuthException.RegistrationConflict)
        assertEquals(
            AuthException.Field.Email,
            (conflict as AuthException.RegistrationConflict).field,
        )
    }
}
