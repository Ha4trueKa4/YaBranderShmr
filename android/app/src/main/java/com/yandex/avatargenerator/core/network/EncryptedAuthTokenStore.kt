package com.yandex.avatargenerator.core.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val PreferencesName = "auth_token_prefs"
private const val KeyAccessToken = "access_token"
private const val LogTag = "EncryptedAuthTokenStore"

/**
 * Токен в [EncryptedSharedPreferences] (ключ — в Android Keystore): переживает
 * перезапуск приложения, поэтому вошедшего пользователя не приходится
 * авторизовывать заново.
 *
 * Хранилище читается **один раз лениво**, дальше значение отдаётся из кэша:
 * `HttpClient` спрашивает токен на каждом запросе, а каждое чтение
 * `EncryptedSharedPreferences` — это расшифровка через Keystore.
 *
 * @param ioDispatcher диспетчер для работы с файлом; в тестах подменяется.
 */
class EncryptedAuthTokenStore(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AuthTokenStore {

    // by lazy: создание MasterKey ходит в Android Keystore и блокирует поток на
    // сотни миллисекунд — нельзя делать это при сборке графа Koin на старте.
    // null — хранилище не поднялось, работаем как «токена нет» (см. createPreferences).
    private val preferences: SharedPreferences? by lazy { createPreferences() }

    private val cache = MutableStateFlow<String?>(null)

    @Volatile
    private var isLoaded = false
    private val loadMutex = Mutex()

    override val token: Flow<String?> = flow {
        loadOnce()
        emitAll(cache)
    }

    override suspend fun currentToken(): String? {
        loadOnce()
        return cache.value
    }

    // Запись берёт тот же [loadMutex], что и чтение: иначе ленивое чтение файла,
    // уже вошедшее в критическую секцию, вернётся из Keystore после update() и
    // затрёт свежий токен старым значением из файла. Инвариант — «чтение из файла
    // никогда не перезаписывает более позднюю запись».
    // Дедлока нет: ни update(), ни clear() не вызывают loadOnce().
    override suspend fun update(token: String) = withContext(ioDispatcher) {
        loadMutex.withLock {
            preferences?.edit { putString(KeyAccessToken, token) }
            isLoaded = true
            cache.value = token
        }
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        loadMutex.withLock {
            preferences?.edit { remove(KeyAccessToken) }
            isLoaded = true
            cache.value = null
        }
    }

    /**
     * Читает файл ровно один раз; [Mutex] — чтобы параллельные запросы не прочли
     * его дважды и, что важнее, чтобы упорядочить чтение из файла относительно
     * записи нового токена: пока идёт чтение (сотни мс в Keystore), [update] и
     * [clear] ждут, и результат чтения не может затереть более свежее значение.
     */
    private suspend fun loadOnce() {
        if (isLoaded) return
        loadMutex.withLock {
            if (isLoaded) return
            cache.value = withContext(ioDispatcher) {
                preferences?.getString(KeyAccessToken, null)
            }
            isLoaded = true
        }
    }

    /**
     * Ключ Keystore может стать невалидным (восстановление бэкапа на другом
     * устройстве, смена блокировки экрана) — тогда `create()` бросает
     * `GeneralSecurityException`/`IOException`. Падать на старте из-за этого
     * нельзя: сносим файл и пробуем ещё раз, а если и это не помогло —
     * возвращаем `null` и живём как «токена нет». Разлогин лучше падения.
     */
    private fun createPreferences(): SharedPreferences? =
        runCatching { openPreferences() }
            .recoverCatching { failure ->
                Log.w(LogTag, "Token storage is unreadable, recreating it", failure)
                context.deleteSharedPreferences(PreferencesName)
                openPreferences()
            }
            .onFailure { failure -> Log.e(LogTag, "Token storage is unavailable", failure) }
            .getOrNull()

    private fun openPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PreferencesName,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
