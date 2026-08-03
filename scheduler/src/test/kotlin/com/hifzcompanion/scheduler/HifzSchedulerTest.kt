package com.hifzcompanion.scheduler

import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.QueueType
import com.hifzcompanion.scheduler.models.ReviewResult
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HifzSchedulerTest {

    private val scheduler = HifzScheduler(maxDailyCapacity = 10)
    private val today = LocalDate.now()

    @Test
    fun `scheduleNext should move NEW to next step on GOOD`() {
        val progress = Progress(
            unitId = 1,
            queueType = QueueType.NEW,
            currentStep = 0,
            nextReviewDate = today,
            lastReviewDate = null,
            lastResult = null
        )

        val next = scheduler.scheduleNext(progress, ReviewResult.GOOD, today)

        assertEquals(1, next.currentStep)
        assertEquals(today.plusDays(1), next.nextReviewDate)
        assertEquals(QueueType.NEW, next.queueType)
    }

    @Test
    fun `scheduleNext should skip a step on STRONG`() {
        val progress = Progress(
            unitId = 1,
            queueType = QueueType.NEW,
            currentStep = 0,
            nextReviewDate = today,
            lastReviewDate = null,
            lastResult = null
        )

        val next = scheduler.scheduleNext(progress, ReviewResult.STRONG, today)

        assertEquals(2, next.currentStep)
        // Interval list: 0, 1, 2, 3...
        // Step 2 is 2 days.
        assertEquals(today.plusDays(2), next.nextReviewDate)
        assertEquals(QueueType.NEW, next.queueType)
    }

    @Test
    fun `scheduleNext should move to LONG_TERM when NEW intervals completed`() {
        val progress = Progress(
            unitId = 1,
            queueType = QueueType.NEW,
            currentStep = Intervals.NEW_MEMORIZATION.size - 1,
            nextReviewDate = today,
            lastReviewDate = null,
            lastResult = null
        )

        val next = scheduler.scheduleNext(progress, ReviewResult.GOOD, today)

        assertEquals(QueueType.LONG_TERM, next.queueType)
        assertEquals(0, next.currentStep)
    }

    @Test
    fun `scheduleNext should restart NEW on FAILED`() {
        val progress = Progress(
            unitId = 1,
            queueType = QueueType.NEW,
            currentStep = 5,
            nextReviewDate = today,
            lastReviewDate = null,
            lastResult = null
        )

        val next = scheduler.scheduleNext(progress, ReviewResult.FAILED, today)

        assertEquals(QueueType.NEW, next.queueType)
        assertEquals(0, next.currentStep)
        assertEquals(today.plusDays(0), next.nextReviewDate)
    }

    @Test
    fun `getDailyTasks should prioritize Recovery over New`() {
        val recoveryTask = Progress(1, QueueType.RECOVERY_FAILED, 0, today, null, null)
        val newTask = Progress(2, QueueType.NEW, 0, today, null, null)
        val longTermTask = Progress(3, QueueType.LONG_TERM, 0, today, null, null)

        val tasks = scheduler.getDailyTasks(listOf(longTermTask, newTask, recoveryTask), today, 0)

        assertEquals(3, tasks.size)
        assertEquals(1, tasks[0].unitId)
        assertEquals(2, tasks[1].unitId)
        assertEquals(3, tasks[2].unitId)
    }

    @Test
    fun `getDailyTasks should limit by capacity`() {
        val manyTasks = (1..20).map { Progress(it, QueueType.NEW, 0, today, null, null) }
        
        // maxDailyCapacity is 10, so 150% is 15
        val tasks = scheduler.getDailyTasks(manyTasks, today, 0)

        assertEquals(15, tasks.size)
    }

    @Test
    fun `calculateLongTermPagesPerDay should follow the formula`() {
        assertEquals(10, scheduler.calculateLongTermPagesPerDay(100)) // 100/14 < 10
        assertEquals(14, scheduler.calculateLongTermPagesPerDay(200)) // 200/14 = 14.28 -> 14
        assertEquals(43, scheduler.calculateLongTermPagesPerDay(604)) // 604/14 = 43.14 -> 43
    }
}
