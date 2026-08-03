package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.scheduler.HifzScheduler
import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.ReviewResult
import com.hifzcompanion.core.domain.models.ReviewType
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import com.hifzcompanion.scheduler.SchedulerConfig
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class RecordReviewUseCase @Inject constructor(
    private val repository: HifzRepository,
    private val scheduler: HifzScheduler,
    private val settingsRepository: UserSettingsRepository
) {
    suspend operator fun invoke(progress: Progress, result: ReviewResult, durationSeconds: Int = 0, notes: String? = null) {
        val today = LocalDate.now()
        val settings = settingsRepository.getUserSettings().first()
        val config = SchedulerConfig(
            dailyCapacity = settings.dailyCapacity,
            minimumPages = settings.minimumPages,
            rotationDays = settings.rotationDays,
            maximumOverloadPercent = settings.maximumOverloadPercent,
            newSchedule = settings.newSchedule,
            weakSchedule = settings.weakSchedule,
            failedSchedule = settings.failedSchedule,
            recoveryStrongReviewsRequired = settings.recoveryStrongReviewsRequired
        )
        val nextProgress = scheduler.scheduleNext(progress, result, today, config)
        repository.updateProgress(nextProgress)
        val type = when (progress.queueType) {
            com.hifzcompanion.scheduler.models.QueueType.NEW -> ReviewType.NEW
            com.hifzcompanion.scheduler.models.QueueType.RECOVERY_WEAK,
            com.hifzcompanion.scheduler.models.QueueType.RECOVERY_FAILED -> ReviewType.RECOVERY
            com.hifzcompanion.scheduler.models.QueueType.LONG_TERM -> ReviewType.SCHEDULED
        }
        repository.recordReview(progress.unitId, result, today, type, durationSeconds, notes)
    }
}
