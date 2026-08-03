package com.hifzcompanion.core.domain.models

import com.hifzcompanion.scheduler.SchedulerConfig
import com.hifzcompanion.scheduler.SchedulerReviewOrder

fun UserSettings.toSchedulerConfig() = SchedulerConfig(
    dailyCapacity = dailyCapacity,
    minimumPages = minimumPages,
    rotationDays = rotationDays,
    maximumOverloadPercent = maximumOverloadPercent,
    newSchedule = newSchedule,
    weakSchedule = weakSchedule,
    failedSchedule = failedSchedule,
    recoveryStrongReviewsRequired = recoveryStrongReviewsRequired,
    reviewOrder = SchedulerReviewOrder.valueOf(reviewOrder.name)
)
