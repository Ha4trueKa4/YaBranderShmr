package com.yandex.avatargenerator.feature.auth.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yandex.avatargenerator.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Пользователь в общем `DataStore<Preferences>` из `coreModule` — своего файла
 * настроек фича не заводит (см. `core/data/AppPreferences.kt`).
 */
internal class AuthUserPreferencesStore(
    private val dataStore: DataStore<Preferences>,
) : AuthUserStore {

    override val user: Flow<AuthUser?> = dataStore.data
        .catch { throwable ->
            // Повреждённый файл настроек не должен ронять приложение:
            // считаем, что сохранённого пользователя нет.
            if (throwable is IOException) emit(emptyPreferences())
            else throw throwable
        }
        .map { preferences ->
            // Без id остальное бессмысленно: экран профиля грузится именно по нему.
            val id = preferences[AUTH_USER_ID] ?: return@map null
            AuthUser(
                id = id,
                email = preferences[AUTH_USER_EMAIL].orEmpty(),
                nickname = preferences[AUTH_USER_NICKNAME].orEmpty(),
            )
        }

    override suspend fun save(user: AuthUser) {
        dataStore.edit { preferences ->
            preferences[AUTH_USER_ID] = user.id
            preferences[AUTH_USER_EMAIL] = user.email
            preferences[AUTH_USER_NICKNAME] = user.nickname
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(AUTH_USER_ID)
            preferences.remove(AUTH_USER_EMAIL)
            preferences.remove(AUTH_USER_NICKNAME)
        }
    }

    private companion object {
        val AUTH_USER_ID = stringPreferencesKey("auth_user_id")
        val AUTH_USER_EMAIL = stringPreferencesKey("auth_user_email")
        val AUTH_USER_NICKNAME = stringPreferencesKey("auth_user_nickname")
    }
}
