package com.hifzcompanion.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.hifzcompanion.core.domain.models.MemorizationOrder
import com.hifzcompanion.core.domain.models.MushafType
import com.hifzcompanion.core.domain.models.AppTheme
import com.hifzcompanion.core.domain.models.ReviewOrder
import com.hifzcompanion.core.domain.models.SessionSortOrder
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
        val REVIEW_ORDER = stringPreferencesKey("review_order")
        val DAILY_CAPACITY = intPreferencesKey("daily_capacity")
        val MINIMUM_PAGES = intPreferencesKey("minimum_pages")
        val ROTATION_DAYS = intPreferencesKey("rotation_days")
        val MAXIMUM_OVERLOAD_PERCENT = intPreferencesKey("maximum_overload_percent")
        val NEW_SCHEDULE = stringPreferencesKey("new_schedule")
        val WEAK_SCHEDULE = stringPreferencesKey("weak_schedule")
        val FAILED_SCHEDULE = stringPreferencesKey("failed_schedule")
        val RECOVERY_STRONG_REVIEWS_REQUIRED = intPreferencesKey("recovery_strong_reviews_required")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val MORNING_NOTIFICATION_TIME = stringPreferencesKey("morning_notification_time")
        val EVENING_NOTIFICATION_TIME = stringPreferencesKey("evening_notification_time")
        val WEEKLY_NOTIFICATION_DAY = intPreferencesKey("weekly_notification_day")
        val AUTOMATIC_BACKUP_ENABLED = booleanPreferencesKey("automatic_backup_enabled")
        val AUTO_ADVANCE = booleanPreferencesKey("auto_advance")
        val THEME = stringPreferencesKey("theme")
        val SESSION_SORT_ORDER = stringPreferencesKey("session_sort_order")
    }

    override fun getUserSettings(): Flow<UserSettings> {
        return dataStore.data.map { preferences ->
            UserSettings(
                onboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: false,
                mushafType = MushafType.valueOf(preferences[Keys.MUSHAF_TYPE] ?: MushafType.MADINAH.name),
                memorizationOrder = MemorizationOrder.valueOf(preferences[Keys.MEMORIZATION_ORDER] ?: MemorizationOrder.BACK_TO_FRONT.name),
                dailyTargetPages = preferences[Keys.DAILY_TARGET_PAGES] ?: 1,
                isAutoTarget = preferences[Keys.IS_AUTO_TARGET] ?: true,
                reviewOrder = enumValueOrDefault(preferences[Keys.REVIEW_ORDER], ReviewOrder.OLDEST_FIRST),
                dailyCapacity = preferences[Keys.DAILY_CAPACITY] ?: 20,
                minimumPages = preferences[Keys.MINIMUM_PAGES] ?: 10,
                rotationDays = preferences[Keys.ROTATION_DAYS] ?: 14,
                maximumOverloadPercent = preferences[Keys.MAXIMUM_OVERLOAD_PERCENT] ?: 150,
                newSchedule = parseSchedule(preferences[Keys.NEW_SCHEDULE], listOf(0, 1, 2, 3, 4, 5, 6, 7, 10, 14, 21, 30)),
                weakSchedule = parseSchedule(preferences[Keys.WEAK_SCHEDULE], listOf(1, 3, 7)),
                failedSchedule = parseSchedule(preferences[Keys.FAILED_SCHEDULE], listOf(0, 1, 3, 7, 14, 30)),
                recoveryStrongReviewsRequired = preferences[Keys.RECOVERY_STRONG_REVIEWS_REQUIRED] ?: 2,
                notificationsEnabled = preferences[Keys.NOTIFICATIONS_ENABLED] ?: true,
                morningNotificationTime = preferences[Keys.MORNING_NOTIFICATION_TIME] ?: "08:00",
                eveningNotificationTime = preferences[Keys.EVENING_NOTIFICATION_TIME] ?: "20:00",
                weeklyNotificationDay = preferences[Keys.WEEKLY_NOTIFICATION_DAY] ?: 7,
                automaticBackupEnabled = preferences[Keys.AUTOMATIC_BACKUP_ENABLED] ?: false,
                autoAdvance = preferences[Keys.AUTO_ADVANCE] ?: true,
                theme = enumValueOrDefault(preferences[Keys.THEME], AppTheme.SYSTEM),
                sessionSortOrder = enumValueOrDefault(preferences[Keys.SESSION_SORT_ORDER], SessionSortOrder.PRIORITY)
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
            preferences[Keys.REVIEW_ORDER] = settings.reviewOrder.name
            preferences[Keys.DAILY_CAPACITY] = settings.dailyCapacity
            preferences[Keys.MINIMUM_PAGES] = settings.minimumPages
            preferences[Keys.ROTATION_DAYS] = settings.rotationDays
            preferences[Keys.MAXIMUM_OVERLOAD_PERCENT] = settings.maximumOverloadPercent
            preferences[Keys.NEW_SCHEDULE] = settings.newSchedule.joinToString(",")
            preferences[Keys.WEAK_SCHEDULE] = settings.weakSchedule.joinToString(",")
            preferences[Keys.FAILED_SCHEDULE] = settings.failedSchedule.joinToString(",")
            preferences[Keys.RECOVERY_STRONG_REVIEWS_REQUIRED] = settings.recoveryStrongReviewsRequired
            preferences[Keys.NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            preferences[Keys.MORNING_NOTIFICATION_TIME] = settings.morningNotificationTime
            preferences[Keys.EVENING_NOTIFICATION_TIME] = settings.eveningNotificationTime
            preferences[Keys.WEEKLY_NOTIFICATION_DAY] = settings.weeklyNotificationDay
            preferences[Keys.AUTOMATIC_BACKUP_ENABLED] = settings.automaticBackupEnabled
            preferences[Keys.AUTO_ADVANCE] = settings.autoAdvance
            preferences[Keys.THEME] = settings.theme.name
            preferences[Keys.SESSION_SORT_ORDER] = settings.sessionSortOrder.name
        }
    }

    private fun parseSchedule(value: String?, default: List<Int>): List<Int> = value
        ?.split(',')
        ?.mapNotNull(String::toIntOrNull)
        ?.takeIf { it.isNotEmpty() }
        ?: default

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default
}
