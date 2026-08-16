package com.yandex.avatargenerator.feature.onboarding.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.yandex.avatargenerator.feature.onboarding.domain.OnboardingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

internal class OnboardingPreferencesRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : OnboardingRepository {

    override val hasSeenOnboarding: Flow<Boolean> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences())
            else throw throwable
        }
        .map { preferences -> preferences[HAS_SEEN_ONBOARDING] == true }

    override suspend fun setOnboardingCompleted() {
        dataStore.edit { preferences -> preferences[HAS_SEEN_ONBOARDING] = true }
    }

    private companion object {
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    }
}
