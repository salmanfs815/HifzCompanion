package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.models.UserSettings
import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.QueueType
import com.hifzcompanion.scheduler.models.SchedulerStage
import java.time.LocalDate
import javax.inject.Inject

class InitializeUserProgressUseCase @Inject constructor(
    private val hifzRepository: HifzRepository,
    private val userSettingsRepository: UserSettingsRepository
) {
    suspend operator fun invoke(settings: UserSettings, memorizedPageIds: List<Int>) {
        // Save settings
        userSettingsRepository.updateUserSettings(settings.copy(onboardingCompleted = true))

        // Initialize progress for memorized pages
        val today = LocalDate.now()
        memorizedPageIds.forEach { pageId ->
            hifzRepository.updateProgress(
                Progress(
                    unitId = pageId,
                    queueType = QueueType.LONG_TERM,
                    stage = SchedulerStage.ROTATING,
                    currentStep = 0,
                    nextReviewDate = today, // Start reviewing from today/tomorrow
                    lastReviewDate = null,
                    lastResult = null,
                    dateMemorized = today
                )
            )
        }
        
        // Initialize next page to memorize if not all pages memorized
        val lastPage = if (settings.memorizationOrder == com.hifzcompanion.core.domain.models.MemorizationOrder.BACK_TO_FRONT) {
            memorizedPageIds.minOrNull() ?: 605
        } else {
            memorizedPageIds.maxOrNull() ?: 0
        }
        
        val nextPage = if (settings.memorizationOrder == com.hifzcompanion.core.domain.models.MemorizationOrder.BACK_TO_FRONT) {
            lastPage - 1
        } else {
            lastPage + 1
        }
        
        if (nextPage in 1..604 && nextPage !in memorizedPageIds) {
             hifzRepository.updateProgress(
                Progress(
                    unitId = nextPage,
                    queueType = QueueType.NEW,
                    stage = SchedulerStage.NEW,
                    currentStep = 0,
                    nextReviewDate = today,
                    lastReviewDate = null,
                    lastResult = null,
                    dateMemorized = null
                )
            )
        }
    }
}
