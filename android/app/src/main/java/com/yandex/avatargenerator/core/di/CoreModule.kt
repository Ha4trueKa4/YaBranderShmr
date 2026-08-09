package com.yandex.avatargenerator.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.yandex.avatargenerator.core.data.appPreferences
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Общие зависимости, не привязанные к одной фиче.
 *
 * Единый [DataStore] на процесс (см. [appPreferences]) — его инъектят репозитории
 * фич, чтобы жить в своём пространстве ключей и не заводить второй файл настроек.
 */
val coreModule = module {
    single<DataStore<Preferences>> { androidContext().appPreferences }
}
