package com.hifzcompanion.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hifzcompanion.core.domain.models.MemorizationOrder
import com.hifzcompanion.core.domain.models.MushafType
import com.hifzcompanion.core.domain.models.UserSettings
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserSettingsRepository {

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val MUSHAF_TYPE = stringPreferencesKey("mushaf_type")
        val MEMORIZATION_ORDER = stringPreferencesKey("memorization_order")
        val DAILY_TARGET_PAGES = intPreferencesKey("daily_target_pages")
        val IS_AUTO_TARGET = booleanPreferencesKey("is_auto_target")
    }

    override fun getUserSettings(): Flow<UserSettings> {
        return dataStore.data.map { preferences ->
            UserSettings(
                onboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false,
                mushafType = MushafType.valueOf(preferences[Keys.MUSHAF_TYPE] ?: MushafType.MADINAH.name),
                memorizationOrder = MemorizationOrder.valueOf(preferences[Keys.MEMORIZATION_ORDER] ?: MemorizationOrder.BACK_TO_FRONT.name),
                dailyTargetPages = preferences[Keys.DAILY_TARGET_PAGES] ?: 1,
                isAutoTarget = preferences[Keys.IS_AUTO_TARGET] ?: true
            )
        }
    }

    override suspend fun updateUserSettings(settings: UserSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.ONBOARDING_COMPLETED] = settings.onboardingCompleted
            preferences[Keys.MUSHAF_TYPE] = settings.mushafType.name
            preferences[Keys.MEMORIZATION_ORDER] = settings.memorizationOrder.name
            preferences[Keys.DAILY_TARGET_PAGES] = settings.dailyTargetPages
            preferences[Keys.IS_AUTO_TARGET] = settings.isAutoTarget
        }
    }
}
