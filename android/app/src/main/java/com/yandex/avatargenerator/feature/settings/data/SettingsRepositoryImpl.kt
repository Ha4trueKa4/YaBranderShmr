package com.yandex.avatargenerator.feature.settings.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.yandex.avatargenerator.core.data.appPreferences
import com.yandex.avatargenerator.feature.settings.domain.SettingsRepository
import com.yandex.avatargenerator.feature.settings.domain.model.AppLanguage
import com.yandex.avatargenerator.feature.settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Хранит тему и язык в общем `DataStore` (см. [SettingsPreferences]).
 * Тема — по имени [ThemeMode], язык — по BCP-47 тегу, чтобы тот же ключ
 * читался синхронно на старте ([SettingsPreferences.currentLanguageTagBlocking]).
 */
internal class SettingsRepositoryImpl(
    private val context: Context,
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = context.appPreferences.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[SettingsPreferences.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.appPreferences.edit { it[SettingsPreferences.THEME_MODE] = mode.name }
    }

    override val language: Flow<AppLanguage> = context.appPreferences.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            AppLanguage.fromTag(prefs[SettingsPreferences.LANGUAGE].orEmpty())
        }

    override suspend fun setLanguage(language: AppLanguage) {
        context.appPreferences.edit { it[SettingsPreferences.LANGUAGE] = language.tag }
    }
}
