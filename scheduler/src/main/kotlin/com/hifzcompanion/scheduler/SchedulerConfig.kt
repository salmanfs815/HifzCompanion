package com.hifzcompanion.scheduler

data class SchedulerConfig(
    val dailyCapacity: Int = 20,
    val minimumPages: Int = 10,
    val rotationDays: Int = 14,
    val maximumOverloadPercent: Int = 150,
    val newSchedule: List<Int> = Intervals.NEW_MEMORIZATION,
    val weakSchedule: List<Int> = Intervals.RECOVERY_WEAK,
    val failedSchedule: List<Int> = Intervals.RECOVERY_FAILED,
    val recoveryStrongReviewsRequired: Int = 2,
    val reviewOrder: SchedulerReviewOrder = SchedulerReviewOrder.OLDEST_FIRST
) {
    init {
        require(dailyCapacity > 0)
        require(minimumPages > 0)
        require(rotationDays > 0)
        require(maximumOverloadPercent >= 100)
        require(newSchedule.isNotEmpty() && weakSchedule.isNotEmpty() && failedSchedule.isNotEmpty())
        require(recoveryStrongReviewsRequired > 0)
    }
}

enum class SchedulerReviewOrder { OLDEST_FIRST, NEWEST_FIRST, PAGE_ORDER }
