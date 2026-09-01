package com.hifzcompanion.scheduler

import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.QueueType
import com.hifzcompanion.scheduler.models.ReviewResult
import com.hifzcompanion.scheduler.models.SchedulerStage
import java.time.LocalDate
import kotlin.math.max

class HifzScheduler(
    private val maxDailyCapacity: Int = 20,
    private val rotationDays: Int = Intervals.LONG_TERM_ROTATION_DEFAULT
) {

    fun scheduleNext(
        progress: Progress,
        result: ReviewResult,
        today: LocalDate,
        config: SchedulerConfig = SchedulerConfig(dailyCapacity = maxDailyCapacity, rotationDays = rotationDays)
    ): Progress {
        return when (result) {
            ReviewResult.STRONG, ReviewResult.GOOD -> {
                val stepIncrement = if (result == ReviewResult.STRONG) 2 else 1
                val newStrength = (progress.strength + if (result == ReviewResult.STRONG) 10 else 4).coerceAtMost(100)
                
                when (progress.queueType) {
                    QueueType.NEW -> {
                        val nextStep = progress.currentStep + stepIncrement
                        if (nextStep < config.newSchedule.size) {
                            progress.copy(
                                stage = SchedulerStage.STABILIZING,
                                currentStep = nextStep,
                                nextReviewDate = today.plusDays(config.newSchedule[nextStep].toLong()),
                                lastReviewDate = today,
                                lastResult = result,
                                strength = newStrength,
                                consecutiveStrongReviews = if (result == ReviewResult.STRONG) progress.consecutiveStrongReviews + 1 else 0
                            )
                        } else {
                            progress.copy(
                                queueType = QueueType.LONG_TERM,
                                stage = SchedulerStage.ROTATING,
                                currentStep = 0,
                                nextReviewDate = today.plusDays(config.rotationDays.toLong()),
                                lastReviewDate = today,
                                lastResult = result,
                                strength = newStrength,
                                consecutiveStrongReviews = 0
                            )
                        }
                    }
                    QueueType.RECOVERY_WEAK -> {
                        val strongCount = if (result == ReviewResult.STRONG) progress.consecutiveStrongReviews + 1 else 0
                        val nextStep = progress.currentStep + 1
                        if (strongCount < config.recoveryStrongReviewsRequired && nextStep < config.weakSchedule.size) {
                            progress.copy(
                                stage = SchedulerStage.RECOVERY,
                                currentStep = nextStep,
                                nextReviewDate = today.plusDays(config.weakSchedule[nextStep].toLong()),
                                lastReviewDate = today,
                                lastResult = result,
                                strength = newStrength,
                                consecutiveStrongReviews = strongCount
                            )
                        } else {
                            progress.copy(
                                queueType = QueueType.LONG_TERM,
                                stage = SchedulerStage.ROTATING,
                                currentStep = 0,
                                nextReviewDate = today.plusDays(config.rotationDays.toLong()),
                                lastReviewDate = today,
                                lastResult = result,
                                strength = newStrength,
                                consecutiveStrongReviews = 0
                            )
                        }
                    }
                    QueueType.RECOVERY_FAILED -> {
                        val strongCount = if (result == ReviewResult.STRONG) progress.consecutiveStrongReviews + 1 else 0
                        val nextStep = progress.currentStep + 1
                        if (strongCount < config.recoveryStrongReviewsRequired && nextStep < config.failedSchedule.size) {
                            progress.copy(
                                stage = SchedulerStage.RECOVERY,
                                currentStep = nextStep,
                                nextReviewDate = today.plusDays(config.failedSchedule[nextStep].toLong()),
                                lastReviewDate = today,
                                lastResult = result,
                                strength = newStrength,
                                consecutiveStrongReviews = strongCount
                            )
                        } else {
                            progress.copy(
                                queueType = QueueType.LONG_TERM,
                                stage = SchedulerStage.ROTATING,
                                currentStep = 0,
                                nextReviewDate = today.plusDays(config.rotationDays.toLong()),
                                lastReviewDate = today,
                                lastResult = result,
                                strength = newStrength,
                                consecutiveStrongReviews = 0
                            )
                        }
                    }
                    QueueType.LONG_TERM -> {
                        val nextInterval = if (result == ReviewResult.STRONG) config.rotationDays * 2 else config.rotationDays
                        progress.copy(
                            stage = SchedulerStage.ROTATING,
                            nextReviewDate = today.plusDays(nextInterval.toLong()),
                            lastReviewDate = today,
                            lastResult = result,
                            strength = newStrength,
                            consecutiveStrongReviews = 0
                        )
                    }
                }
            }
            ReviewResult.WEAK -> {
                progress.copy(
                    queueType = QueueType.RECOVERY_WEAK,
                    stage = SchedulerStage.WEAK,
                    currentStep = 0,
                    nextReviewDate = today.plusDays(config.weakSchedule[0].toLong()),
                    lastReviewDate = today,
                    lastResult = result,
                    strength = (progress.strength - 15).coerceAtLeast(0),
                    consecutiveStrongReviews = 0
                )
            }
            ReviewResult.FAILED -> {
                progress.copy(
                    queueType = QueueType.RECOVERY_FAILED,
                    stage = SchedulerStage.RECOVERY,
                    currentStep = 0,
                    nextReviewDate = today.plusDays(config.failedSchedule[0].toLong()),
                    lastReviewDate = today,
                    lastResult = result,
                    strength = (progress.strength - 30).coerceAtLeast(0),
                    consecutiveStrongReviews = 0
                )
            }
        }
    }

    fun getDailyTasks(
        allProgress: List<Progress>,
        today: LocalDate,
        memorizedPagesCount: Int,
        config: SchedulerConfig = SchedulerConfig(dailyCapacity = maxDailyCapacity, rotationDays = rotationDays)
    ): List<Progress> {
        val dueTasks = allProgress.filter {
            it.active &&
                it.dateMemorized != null &&
                it.lastReviewDate != today &&
                it.nextReviewDate <= today
        }
        
        val recovery = dueTasks.filter { it.queueType == QueueType.RECOVERY_WEAK || it.queueType == QueueType.RECOVERY_FAILED }
            .sortedBy { it.nextReviewDate }
        
        val newTasks = dueTasks.filter { it.queueType == QueueType.NEW }
            .sortedBy { it.nextReviewDate }
            
        val longTermCandidates = dueTasks.filter { it.queueType == QueueType.LONG_TERM }
        val longTerm = when (config.reviewOrder) {
            SchedulerReviewOrder.OLDEST_FIRST -> longTermCandidates.sortedWith(compareBy<Progress> { it.dateMemorized }.thenBy { it.nextReviewDate })
            SchedulerReviewOrder.NEWEST_FIRST -> longTermCandidates.sortedWith(compareByDescending<Progress> { it.dateMemorized }.thenBy { it.nextReviewDate })
            SchedulerReviewOrder.PAGE_ORDER -> longTermCandidates.sortedBy { it.unitId }
        }

        val maxCapacity = maximumDailyTasks(config)
        val priorityTasks = recovery + newTasks
        val remainingCapacity = (maxCapacity - priorityTasks.size).coerceAtLeast(0)
        val longTermTarget = calculateLongTermPagesPerDay(memorizedPagesCount, config)
        return (priorityTasks + longTerm.take(maxOf(remainingCapacity, longTermTarget)))
            .distinctBy { it.unitId }
            .take(maxCapacity)
    }

    fun maximumDailyTasks(
        config: SchedulerConfig = SchedulerConfig(dailyCapacity = maxDailyCapacity, rotationDays = rotationDays)
    ): Int = maxOf(
        config.dailyCapacity,
        (config.dailyCapacity * config.maximumOverloadPercent / 100.0).toInt()
    )

    fun calculateLongTermPagesPerDay(
        memorizedPages: Int,
        config: SchedulerConfig = SchedulerConfig(dailyCapacity = maxDailyCapacity, rotationDays = rotationDays)
    ): Int {
        return max(config.minimumPages, memorizedPages / config.rotationDays)
    }
}
