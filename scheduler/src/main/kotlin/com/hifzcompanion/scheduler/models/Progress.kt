package com.hifzcompanion.scheduler.models

import java.time.LocalDate

data class Progress(
    val unitId: Int,
    val queueType: QueueType,
    val currentStep: Int, // Index in the interval list
    val nextReviewDate: LocalDate,
    val lastReviewDate: LocalDate?,
    val lastResult: ReviewResult?
)

enum class QueueType {
    NEW, LONG_TERM, RECOVERY_WEAK, RECOVERY_FAILED
}
