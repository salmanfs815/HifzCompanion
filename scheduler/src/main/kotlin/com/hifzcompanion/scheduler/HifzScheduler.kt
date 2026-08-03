package com.hifzcompanion.scheduler

import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.QueueType
import com.hifzcompanion.scheduler.models.ReviewResult
import java.time.LocalDate
import kotlin.math.max

class HifzScheduler(
    private val maxDailyCapacity: Int = 20,
    private val rotationDays: Int = Intervals.LONG_TERM_ROTATION_DEFAULT
) {

    fun scheduleNext(progress: Progress, result: ReviewResult, today: LocalDate): Progress {
        return when (result) {
            ReviewResult.STRONG, ReviewResult.GOOD -> {
                val stepIncrement = if (result == ReviewResult.STRONG) 2 else 1
                
                when (progress.queueType) {
                    QueueType.NEW -> {
                        val nextStep = progress.currentStep + stepIncrement
                        if (nextStep < Intervals.NEW_MEMORIZATION.size) {
                            progress.copy(
                                currentStep = nextStep,
                                nextReviewDate = today.plusDays(Intervals.NEW_MEMORIZATION[nextStep].toLong()),
                                lastReviewDate = today,
                                lastResult = result
                            )
                        } else {
                            progress.copy(
                                queueType = QueueType.LONG_TERM,
                                currentStep = 0,
                                nextReviewDate = today.plusDays(rotationDays.toLong()),
                                lastReviewDate = today,
                                lastResult = result
                            )
                        }
                    }
                    QueueType.RECOVERY_WEAK -> {
                        val nextStep = progress.currentStep + stepIncrement
                        if (nextStep < Intervals.RECOVERY_WEAK.size) {
                            progress.copy(
                                currentStep = nextStep,
                                nextReviewDate = today.plusDays(Intervals.RECOVERY_WEAK[nextStep].toLong()),
                                lastReviewDate = today,
                                lastResult = result
                            )
                        } else {
                            progress.copy(
                                queueType = QueueType.LONG_TERM,
                                currentStep = 0,
                                nextReviewDate = today.plusDays(rotationDays.toLong()),
                                lastReviewDate = today,
                                lastResult = result
                            )
                        }
                    }
                    QueueType.RECOVERY_FAILED -> {
                        val nextStep = progress.currentStep + stepIncrement
                        if (nextStep < Intervals.RECOVERY_FAILED.size) {
                            progress.copy(
                                currentStep = nextStep,
                                nextReviewDate = today.plusDays(Intervals.RECOVERY_FAILED[nextStep].toLong()),
                                lastReviewDate = today,
                                lastResult = result
                            )
                        } else {
                            progress.copy(
                                queueType = QueueType.LONG_TERM,
                                currentStep = 0,
                                nextReviewDate = today.plusDays(rotationDays.toLong()),
                                lastReviewDate = today,
                                lastResult = result
                            )
                        }
                    }
                    QueueType.LONG_TERM -> {
                        val nextInterval = if (result == ReviewResult.STRONG) rotationDays * 2 else rotationDays
                        progress.copy(
                            nextReviewDate = today.plusDays(nextInterval.toLong()),
                            lastReviewDate = today,
                            lastResult = result
                        )
                    }
                }
            }
            ReviewResult.WEAK -> {
                progress.copy(
                    queueType = QueueType.RECOVERY_WEAK,
                    currentStep = 0,
                    nextReviewDate = today.plusDays(Intervals.RECOVERY_WEAK[0].toLong()),
                    lastReviewDate = today,
                    lastResult = result
                )
            }
            ReviewResult.FAILED -> {
                // Restart as newly memorized
                progress.copy(
                    queueType = QueueType.NEW,
                    currentStep = 0,
                    nextReviewDate = today.plusDays(Intervals.NEW_MEMORIZATION[0].toLong()),
                    lastReviewDate = today,
                    lastResult = result
                )
            }
        }
    }

    fun getDailyTasks(
        allProgress: List<Progress>,
        today: LocalDate,
        memorizedPagesCount: Int
    ): List<Progress> {
        val dueTasks = allProgress.filter { it.nextReviewDate <= today }
        
        val recovery = dueTasks.filter { it.queueType == QueueType.RECOVERY_WEAK || it.queueType == QueueType.RECOVERY_FAILED }
            .sortedBy { it.nextReviewDate }
        
        val newTasks = dueTasks.filter { it.queueType == QueueType.NEW }
            .sortedBy { it.nextReviewDate }
            
        val longTerm = dueTasks.filter { it.queueType == QueueType.LONG_TERM }
            .sortedBy { it.nextReviewDate }

        val prioritizedTasks = recovery + newTasks + longTerm
        
        // Load Balancing: Max 150% capacity
        val maxCapacity = (maxDailyCapacity * 1.5).toInt()
        
        return prioritizedTasks.take(maxCapacity)
    }

    fun calculateLongTermPagesPerDay(memorizedPages: Int): Int {
        return max(10, memorizedPages / rotationDays)
    }
}
