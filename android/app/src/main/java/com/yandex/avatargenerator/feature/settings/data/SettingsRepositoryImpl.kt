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
import kotlinx.coroutines.flow.onEach
import java.io.IOException

/**
 * Тема и язык хранятся в общем `DataStore` (см. [SettingsPreferences]). Оба применяются
 * вживую на корне Compose (тема — через палитру, язык — через подмену локали в
 * `LocalContext`), поэтому смена не пересоздаёт Activity. Для первого кадра после старта
 * держим синхронный кэш последних значений ([lastThemeMode]/[lastLanguage]).
 */
internal class SettingsRepositoryImpl(
    private val context: Context,
) : SettingsRepository {

    // Volatile: читается из MainActivity.setContent (main), пишется из корутины-коллектора.
    @Volatile
    private var _lastThemeMode: ThemeMode = ThemeMode.SYSTEM
    override val lastThemeMode: ThemeMode get() = _lastThemeMode

    @Volatile
    private var _lastLanguage: AppLanguage = AppLanguage.SYSTEM
    override val lastLanguage: AppLanguage get() = _lastLanguage

    override val themeMode: Flow<ThemeMode> = context.appPreferences.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            prefs[SettingsPreferences.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM
        }
        .onEach { _lastThemeMode = it }

    override suspend fun setThemeMode(mode: ThemeMode) {
        _lastThemeMode = mode
        context.appPreferences.edit { it[SettingsPreferences.THEME_MODE] = mode.name }
    }

    override val language: Flow<AppLanguage> = context.appPreferences.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> AppLanguage.fromTag(prefs[SettingsPreferences.LANGUAGE].orEmpty()) }
        .onEach { _lastLanguage = it }

    override suspend fun setLanguage(language: AppLanguage) {
        _lastLanguage = language
        context.appPreferences.edit { it[SettingsPreferences.LANGUAGE] = language.tag }
    }
}
