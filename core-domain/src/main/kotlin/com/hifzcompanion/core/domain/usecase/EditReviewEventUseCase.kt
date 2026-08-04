package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.models.ReviewRecord
import com.hifzcompanion.core.domain.models.ReviewType
import com.hifzcompanion.core.domain.models.toSchedulerConfig
import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import com.hifzcompanion.scheduler.HifzScheduler
import com.hifzcompanion.scheduler.models.*
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class EditReviewEventUseCase @Inject constructor(
    private val repository: HifzRepository,
    private val settingsRepository: UserSettingsRepository,
    private val scheduler: HifzScheduler
) {
    suspend operator fun invoke(event: ReviewRecord, correctedQuality: ReviewResult) {
        if (event.quality == correctedQuality) return
        repository.updateReviewEventQuality(event.id, correctedQuality)

        val history = repository.getReviewEventsSnapshot(event.pageId)
        val current = repository.getProgress(event.pageId).first() ?: return
        val config = settingsRepository.getUserSettings().first().toSchedulerConfig()
        val firstEvent = history.firstOrNull() ?: return
        val firstDate = firstEvent.reviewDate()
        var replayed = Progress(
            unitId = event.pageId,
            queueType = if (firstEvent.reviewType == ReviewType.NEW) QueueType.NEW else QueueType.LONG_TERM,
            currentStep = 0,
            nextReviewDate = firstDate,
            lastReviewDate = null,
            lastResult = null,
            stage = if (firstEvent.reviewType == ReviewType.NEW) SchedulerStage.NEW else SchedulerStage.ROTATING,
            dateMemorized = current.dateMemorized ?: firstDate,
            active = current.active,
            notes = current.notes
        )

        history.forEach { review ->
            val reviewDate = review.reviewDate()
            replayed = scheduler.scheduleNext(replayed, review.quality, reviewDate, config)
            if (review.reviewType == ReviewType.MANUAL && replayed.nextReviewDate <= reviewDate) {
                replayed = replayed.copy(nextReviewDate = reviewDate.plusDays(1))
            }
        }
        repository.updateProgress(replayed.copy(active = current.active, notes = current.notes))
    }

    private fun ReviewRecord.reviewDate(): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
}
