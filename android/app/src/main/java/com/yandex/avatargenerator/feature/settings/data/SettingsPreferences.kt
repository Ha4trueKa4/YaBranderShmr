package com.yandex.avatargenerator.feature.settings.data

import android.content.Context
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yandex.avatargenerator.core.data.appPreferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.IOException

/**
 * Ключи настроек в общем `DataStore` и синхронное чтение локали для старта.
 */
internal object SettingsPreferences {
    val THEME_MODE = stringPreferencesKey("settings.theme_mode")
    val LANGUAGE = stringPreferencesKey("settings.language")

    /**
     * Тег выбранной локали (пустой — «как в системе») для `attachBaseContext`,
     * где нужно синхронно, ещё до старта Compose. Блокирующее чтение маленького
     * файла настроек идёт на главном потоке при каждом создании Activity (в т.ч.
     * пересоздании после смены языка) — осознанный компромисс: применить локаль к
     * ресурсам иначе негде. Повреждённый файл не роняет запуск (возвращаем «система»).
     */
    fun currentLanguageTagBlocking(context: Context): String = runBlocking {
        context.appPreferences.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .first()[LANGUAGE].orEmpty()
    }
}
