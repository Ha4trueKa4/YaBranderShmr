package com.yandex.avatargenerator.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val PREFERENCES_NAME = "avatar_generator_prefs"

/**
 * Общее хранилище флагов и настроек приложения.
 *
 * Делегат [preferencesDataStore] объявлен ровно один раз на процесс: второе
 * объявление с тем же именем файла упало бы в рантайме. Поэтому фичи не заводят
 * свои DataStore, а берут этот и живут в своём пространстве ключей.
 */
internal val Context.appPreferences: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)
