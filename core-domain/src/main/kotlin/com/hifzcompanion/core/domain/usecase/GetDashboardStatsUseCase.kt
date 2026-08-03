package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.scheduler.HifzScheduler
import com.hifzcompanion.scheduler.models.QueueType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import com.hifzcompanion.core.domain.models.toSchedulerConfig
import com.hifzcompanion.scheduler.models.ReviewResult
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.LocalDate
import javax.inject.Inject

data class DashboardStats(
    val todayProgressPages: Int,
    val totalProgressPages: Int,
    val todayReviewCount: Int,
    val newMemorizationCount: Int,
    val recoveryCount: Int,
    val overdueCount: Int,
    val currentStreak: Int,
    val totalMemorizedPages: Int,
    val pagesDueToday: Int,
    val healthScore: Int,
    val averageQuality: Double,
    val longestStreak: Int,
    val missedReviewDays: Int,
    val backlogCount: Int,
    val averageReviewsPerDay: Double
)

class GetDashboardStatsUseCase @Inject constructor(
    private val repository: HifzRepository,
    private val scheduler: HifzScheduler,
    private val settingsRepository: UserSettingsRepository
) {
    operator fun invoke(): Flow<DashboardStats> {
        val today = LocalDate.now()
        return combine(
            repository.getAllProgress(),
            repository.getTodayReviewCount(),
            repository.getAllReviewEvents(),
            settingsRepository.getUserSettings()
        ) { allProgress, todayCount, events, settings ->
            val memorized = allProgress.filter { it.dateMemorized != null }
            val dailyTasks = scheduler.getDailyTasks(allProgress, today, memorized.size, settings.toSchedulerConfig())
            
            val totalMemorized = memorized.size
            val overdue = memorized.count { it.nextReviewDate < today }
            val dueToday = memorized.count { it.nextReviewDate == today }
            
            val todayReview = dailyTasks.size
            val newMemorization = dailyTasks.count { it.queueType == QueueType.NEW }
            val recovery = dailyTasks.count { it.queueType == QueueType.RECOVERY_WEAK || it.queueType == QueueType.RECOVERY_FAILED }

            val qualityValues = events.map {
                when (it.quality) {
                    ReviewResult.STRONG -> 4
                    ReviewResult.GOOD -> 3
                    ReviewResult.WEAK -> 2
                    ReviewResult.FAILED -> 1
                }
            }
            val averageQuality = qualityValues.average().takeUnless { it.isNaN() } ?: 0.0
            val reviewDates = events.map {
                Instant.ofEpochMilli(it.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
            }.distinct().sorted()
            val streaks = mutableListOf<Int>()
            var streak = 0
            var previous: LocalDate? = null
            reviewDates.forEach { date ->
                streak = if (previous != null && ChronoUnit.DAYS.between(previous, date) == 1L) streak + 1 else 1
                streaks += streak
                previous = date
            }
            val currentStreak = if (reviewDates.lastOrNull()?.let { ChronoUnit.DAYS.between(it, today) <= 1 } == true) streak else 0
            val longestStreak = streaks.maxOrNull() ?: 0
            val firstDate = reviewDates.firstOrNull()
            val elapsedDays = firstDate?.let { ChronoUnit.DAYS.between(it, today).toInt() + 1 } ?: 0
            val missedDays = (elapsedDays - reviewDates.size).coerceAtLeast(0)
            val averageReviews = if (elapsedDays > 0) events.size.toDouble() / elapsedDays else 0.0
            val weakCount = memorized.count { it.lastResult == ReviewResult.WEAK }
            val failedCount = memorized.count { it.lastResult == ReviewResult.FAILED }
            val consistencyBonus = (currentStreak * 2).coerceAtMost(10)
            val healthScore = (100 - overdue * 2 - weakCount * 3 - failedCount * 5 - missedDays + consistencyBonus).coerceIn(0, 100)

            DashboardStats(
                todayProgressPages = todayCount,
                totalProgressPages = todayReview + todayCount, // Those due + those already done
                todayReviewCount = todayReview,
                newMemorizationCount = newMemorization,
                recoveryCount = recovery,
                overdueCount = overdue,
                currentStreak = currentStreak,
                totalMemorizedPages = totalMemorized,
                pagesDueToday = dueToday,
                healthScore = healthScore,
                averageQuality = averageQuality,
                longestStreak = longestStreak,
                missedReviewDays = missedDays,
                backlogCount = overdue,
                averageReviewsPerDay = averageReviews
            )
        }
    }
}
