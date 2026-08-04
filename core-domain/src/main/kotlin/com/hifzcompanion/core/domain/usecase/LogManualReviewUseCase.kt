package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.models.ReviewType
import com.hifzcompanion.core.domain.models.toSchedulerConfig
import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import com.hifzcompanion.scheduler.HifzScheduler
import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.QueueType
import com.hifzcompanion.scheduler.models.ReviewResult
import com.hifzcompanion.scheduler.models.SchedulerStage
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class LogManualReviewUseCase @Inject constructor(
    private val repository: HifzRepository,
    private val settingsRepository: UserSettingsRepository,
    private val scheduler: HifzScheduler
) {
    suspend operator fun invoke(
        pageIds: Collection<Int>,
        result: ReviewResult,
        newlyMemorized: Boolean,
        notes: String? = null
    ) {
        val today = LocalDate.now()
        val config = settingsRepository.getUserSettings().first().toSchedulerConfig()

        pageIds.filter { it in 1..604 }.distinct().forEach { page ->
            val current = repository.getProgress(page).first()
            val starting = when {
                newlyMemorized -> (current ?: blankProgress(page, today)).copy(
                    queueType = QueueType.NEW,
                    stage = SchedulerStage.NEW,
                    currentStep = 0,
                    nextReviewDate = today,
                    dateMemorized = today,
                    active = true
                )
                current == null || !current.active || current.dateMemorized == null -> blankProgress(page, today)
                else -> current
            }
            val scheduled = scheduler.scheduleNext(starting, result, today, config)
            repository.updateProgress(
                scheduled.copy(
                    active = true,
                    dateMemorized = scheduled.dateMemorized ?: today,
                    // A manually completed page must disappear from today's due queue.
                    nextReviewDate = if (scheduled.nextReviewDate <= today) today.plusDays(1) else scheduled.nextReviewDate
                )
            )
            repository.recordReview(page, result, today, ReviewType.MANUAL, notes = notes)
        }
    }

    private fun blankProgress(page: Int, today: LocalDate) = Progress(
        unitId = page,
        queueType = QueueType.LONG_TERM,
        currentStep = 0,
        nextReviewDate = today,
        lastReviewDate = null,
        lastResult = null,
        stage = SchedulerStage.ROTATING,
        dateMemorized = today
    )
}
