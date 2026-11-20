package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.scheduler.HifzScheduler
import com.hifzcompanion.scheduler.models.QueueType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    val health: HealthBreakdown,
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
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<DashboardStats> {
        return systemDateFlow().flatMapLatest { today ->
            combine(
                repository.getAllProgress(),
                repository.getReviewCountForDate(today),
                repository.getAllReviewEvents(),
                settingsRepository.getUserSettings()
            ) { allProgress, rawTodayCount, events, settings ->
                val memorized = allProgress.filter { it.active && it.dateMemorized != null }
                val config = settings.toSchedulerConfig()
                val maximum = scheduler.maximumDailyTasks(config)
                val todayCount = rawTodayCount.coerceAtMost(maximum)
                val dailyTasks = scheduler.getDailyTasks(allProgress, today, memorized.size, config)
                    .take((maximum - todayCount).coerceAtLeast(0))
            
            val totalMemorized = memorized.size
            val overdue = memorized.count { it.nextReviewDate < today }
            val dueToday = memorized.count { it.nextReviewDate == today }
            
            val todayReview = dailyTasks.size
            val newMemorization = dailyTasks.count { it.queueType == QueueType.NEW }
            // Match the Recovery card's destination, including pages moved into
            // recovery by a review today whose next review is on a future date.
            val recovery = allProgress.count {
                it.active && (it.queueType == QueueType.RECOVERY_WEAK || it.queueType == QueueType.RECOVERY_FAILED)
            }

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
            val health = HealthBreakdown.calculate(
                memorizedPages = totalMemorized,
                daysOverdue = memorized.map { ChronoUnit.DAYS.between(it.nextReviewDate, today) },
                weakPages = weakCount,
                failedPages = failedCount,
                reviewDates = reviewDates,
                today = today
            )

                DashboardStats(
                    todayProgressPages = todayCount,
                    totalProgressPages = todayReview + todayCount,
                    todayReviewCount = todayReview,
                    newMemorizationCount = newMemorization,
                    recoveryCount = recovery,
                    overdueCount = overdue,
                    currentStreak = currentStreak,
                    totalMemorizedPages = totalMemorized,
                    pagesDueToday = dueToday,
                    healthScore = health.score,
                    health = health,
                    averageQuality = averageQuality,
                    longestStreak = longestStreak,
                    missedReviewDays = missedDays,
                    backlogCount = overdue,
                    averageReviewsPerDay = averageReviews
                )
            }
        }
    }
}
