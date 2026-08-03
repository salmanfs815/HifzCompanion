package com.hifzcompanion.scheduler.models

import java.time.LocalDate

data class Progress(
    val unitId: Int,
    val queueType: QueueType,
    val currentStep: Int,
    val nextReviewDate: LocalDate,
    val lastReviewDate: LocalDate?,
    val lastResult: ReviewResult?,
    val stage: SchedulerStage = SchedulerStage.NEW,
    val dateMemorized: LocalDate? = null,
    val strength: Int = 50,
    val consecutiveStrongReviews: Int = 0,
    val active: Boolean = true,
    val notes: String? = null
)

enum class QueueType {
    NEW, LONG_TERM, RECOVERY_WEAK, RECOVERY_FAILED
}

enum class SchedulerStage {
    NEW, STABILIZING, ROTATING, WEAK, RECOVERY
}
