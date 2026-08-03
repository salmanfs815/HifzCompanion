package com.hifzcompanion.core.domain.repository

import com.hifzcompanion.core.domain.models.UserSettings
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepository {
    fun getUserSettings(): Flow<UserSettings>
    suspend fun updateUserSettings(settings: UserSettings)
}
