package com.hifzcompanion.core.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val mushafType: MushafType = MushafType.MADINAH,
    val memorizationOrder: MemorizationOrder = MemorizationOrder.BACK_TO_FRONT,
    val dailyTargetPages: Int = 1,
    val isAutoTarget: Boolean = true,
    val reviewOrder: ReviewOrder = ReviewOrder.OLDEST_FIRST,
    val dailyCapacity: Int = 20,
    val minimumPages: Int = 10,
    val rotationDays: Int = 14,
    val maximumOverloadPercent: Int = 150,
    val newSchedule: List<Int> = listOf(0, 1, 2, 3, 4, 5, 6, 7, 10, 14, 21, 30),
    val weakSchedule: List<Int> = listOf(1, 3, 7),
    val failedSchedule: List<Int> = listOf(0, 1, 3, 7, 14, 30),
    val recoveryStrongReviewsRequired: Int = 2,
    val notificationsEnabled: Boolean = true,
    val morningNotificationTime: String = "08:00",
    val eveningNotificationTime: String = "20:00",
    val weeklyNotificationDay: Int = 7,
    val automaticBackupEnabled: Boolean = false,
    val autoAdvance: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val sessionSortOrder: SessionSortOrder = SessionSortOrder.PRIORITY
)

/**
 * Page layouts. Only [MADINAH] has page data ([com.hifzcompanion.core.domain.quran.QuranMetadata]);
 * [URDU] and [TAJWEED] remain so settings and backups saved by earlier versions still load.
 */
@Serializable
enum class MushafType(val label: String) {
    MADINAH("Madinah (15-line, 604 pages)"),
    URDU("Indopak"),
    TAJWEED("Tajweed");

    companion object {
        /** Layouts the app has page data for, offered during onboarding. */
        val supported: List<MushafType> = listOf(MADINAH)
    }
}

@Serializable
enum class MemorizationOrder {
    FRONT_TO_BACK, BACK_TO_FRONT, PAGE_ORDER, CUSTOM
}

@Serializable
enum class ReviewOrder { OLDEST_FIRST, NEWEST_FIRST, PAGE_ORDER }

@Serializable
enum class AppTheme { SYSTEM, LIGHT, DARK }

/** Display order of today's already-selected review pages; does not change which pages are selected. */
@Serializable
enum class SessionSortOrder { PRIORITY, PAGE_NUMBER }
