package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.scheduler.HifzScheduler
import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.ReviewResult
import java.time.LocalDate
import javax.inject.Inject

class RecordReviewUseCase @Inject constructor(
    private val repository: HifzRepository,
    private val scheduler: HifzScheduler
) {
    suspend operator fun invoke(progress: Progress, result: ReviewResult) {
        val today = LocalDate.now()
        
        // 1. Calculate next state
        val nextProgress = scheduler.scheduleNext(progress, result, today)
        
        // 2. Update Progress in DB
        repository.updateProgress(nextProgress)
        
        // 3. Record Review Event
        repository.recordReview(progress.unitId, result, today)
        
        // TODO: Update Streaks and Health Score if needed (can be reactive based on DB changes)
    }
}
