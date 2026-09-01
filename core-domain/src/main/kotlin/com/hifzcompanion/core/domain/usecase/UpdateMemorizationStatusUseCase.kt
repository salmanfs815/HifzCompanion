package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import com.hifzcompanion.core.domain.models.MemorizationOrder
import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.QueueType
import com.hifzcompanion.scheduler.models.SchedulerStage
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class UpdateMemorizationStatusUseCase @Inject constructor(
    private val repository: HifzRepository,
    private val settingsRepository: UserSettingsRepository
) {
    suspend operator fun invoke(selectedPageIds: Collection<Int>) {
        val selected = selectedPageIds.filter { it in 1..604 }.toSet()
        val existing = repository.getAllProgress().first().associateBy { it.unitId }
        val today = LocalDate.now()

        existing.values.forEach { progress ->
            when {
                progress.unitId in selected && (progress.dateMemorized == null || !progress.active) ->
                    repository.updateProgress(progress.asMemorized(today))
                progress.unitId !in selected && progress.dateMemorized != null ->
                    repository.deleteProgressAndHistory(progress.unitId)
                progress.unitId !in selected && progress.active ->
                    repository.updateProgress(progress.copy(active = false))
            }
        }

        (selected - existing.keys).forEach { page ->
            repository.updateProgress(
                Progress(
                    unitId = page,
                    queueType = QueueType.LONG_TERM,
                    currentStep = 0,
                    nextReviewDate = today,
                    lastReviewDate = null,
                    lastResult = null,
                    stage = SchedulerStage.ROTATING,
                    dateMemorized = today
                )
            )
        }

        val order = settingsRepository.getUserSettings().first().memorizationOrder
        val nextPage = when (order) {
            MemorizationOrder.BACK_TO_FRONT -> (selected.minOrNull() ?: 605) - 1
            else -> (selected.maxOrNull() ?: 0) + 1
        }
        if (nextPage in 1..604 && nextPage !in selected) {
            repository.updateProgress(
                (existing[nextPage] ?: Progress(
                    unitId = nextPage,
                    queueType = QueueType.NEW,
                    currentStep = 0,
                    nextReviewDate = today,
                    lastReviewDate = null,
                    lastResult = null
                )).copy(
                    queueType = QueueType.NEW,
                    stage = SchedulerStage.NEW,
                    currentStep = 0,
                    nextReviewDate = today,
                    lastReviewDate = null,
                    lastResult = null,
                    dateMemorized = null,
                    active = true
                )
            )
        }
    }

    private fun Progress.asMemorized(today: LocalDate) = copy(
        queueType = QueueType.LONG_TERM,
        stage = SchedulerStage.ROTATING,
        currentStep = 0,
        nextReviewDate = today,
        dateMemorized = dateMemorized ?: today,
        active = true
    )
}
