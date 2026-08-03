package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.scheduler.HifzScheduler
import com.hifzcompanion.scheduler.models.QueueType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    val healthScore: Int
)

class GetDashboardStatsUseCase @Inject constructor(
    private val repository: HifzRepository,
    private val scheduler: HifzScheduler
) {
    operator fun invoke(): Flow<DashboardStats> {
        val today = LocalDate.now()
        return combine(
            repository.getAllProgress(),
            repository.getTodayReviewCount()
        ) { allProgress, todayCount ->
            val dailyTasks = scheduler.getDailyTasks(allProgress, today, allProgress.count { it.lastReviewDate != null })
            
            val totalMemorized = allProgress.count { it.lastReviewDate != null }
            val overdue = allProgress.count { it.nextReviewDate < today }
            val dueToday = allProgress.count { it.nextReviewDate == today }
            
            val todayReview = dailyTasks.size
            val newMemorization = dailyTasks.count { it.queueType == QueueType.NEW }
            val recovery = dailyTasks.count { it.queueType == QueueType.RECOVERY_WEAK || it.queueType == QueueType.RECOVERY_FAILED }

            // Placeholder logic for streak and health score
            val healthScore = if (totalMemorized > 0) {
                ((totalMemorized - overdue).toFloat() / totalMemorized * 100).toInt().coerceIn(0, 100)
            } else 100

            DashboardStats(
                todayProgressPages = todayCount,
                totalProgressPages = todayReview + todayCount, // Those due + those already done
                todayReviewCount = todayReview,
                newMemorizationCount = newMemorization,
                recoveryCount = recovery,
                overdueCount = overdue,
                currentStreak = 0, // Placeholder
                totalMemorizedPages = totalMemorized,
                pagesDueToday = dueToday,
                healthScore = healthScore
            )
        }
    }
}
