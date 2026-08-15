package com.yandex.avatargenerator.core.network

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val PreferencesName = "auth_token_prefs"

/**
 * Тест хранилища токена. Инструментальный, а не юнит: `EncryptedSharedPreferences`
 * шифрует ключом из Android Keystore, которого на JVM нет.
 */
@RunWith(AndroidJUnit4::class)
class EncryptedAuthTokenStoreTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        context.deleteSharedPreferences(PreferencesName)
    }

    @After
    fun tearDown() {
        context.deleteSharedPreferences(PreferencesName)
    }

    @Test
    fun storedTokenIsReadBack() = runBlocking {
        val store = EncryptedAuthTokenStore(context)

        store.update("token-1")

        assertEquals("token-1", store.currentToken())
    }

    @Test
    fun emptyStorageHasNoToken() = runBlocking {
        val store = EncryptedAuthTokenStore(context)

        assertNull(store.currentToken())
    }

    @Test
    fun clearRemovesToken() = runBlocking {
        val store = EncryptedAuthTokenStore(context)
        store.update("token-1")

        store.clear()

        assertNull(store.currentToken())
    }

    /** Ключевая проверка задачи: токен лежит в файле, а не только в кэше экземпляра. */
    @Test
    fun tokenSurvivesStoreRecreation() = runBlocking {
        EncryptedAuthTokenStore(context).update("token-1")

        val restored = EncryptedAuthTokenStore(context).currentToken()

        assertEquals("token-1", restored)
    }

    @Test
    fun clearedTokenDoesNotComeBackAfterRecreation() = runBlocking {
        val store = EncryptedAuthTokenStore(context)
        store.update("token-1")
        store.clear()

        assertNull(EncryptedAuthTokenStore(context).currentToken())
    }
}
