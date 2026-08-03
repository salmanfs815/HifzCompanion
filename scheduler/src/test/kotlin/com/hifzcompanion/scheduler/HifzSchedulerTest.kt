package com.hifzcompanion.scheduler

import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.QueueType
import com.hifzcompanion.scheduler.models.ReviewResult
import com.hifzcompanion.scheduler.models.SchedulerStage
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

    @Test
    fun `custom schedules and rotation are honored`() {
        val config = SchedulerConfig(
            dailyCapacity = 12,
            minimumPages = 5,
            rotationDays = 21,
            newSchedule = listOf(0, 4, 9),
            weakSchedule = listOf(2, 5),
            failedSchedule = listOf(0, 2, 6)
        )
        val progress = Progress(1, QueueType.NEW, 0, today, null, null)

        val next = scheduler.scheduleNext(progress, ReviewResult.GOOD, today, config)

        assertEquals(today.plusDays(4), next.nextReviewDate)
        assertEquals(SchedulerStage.STABILIZING, next.stage)
    }

    @Test
    fun `weak result enters recovery and reduces strength`() {
        val progress = Progress(1, QueueType.LONG_TERM, 0, today, today.minusDays(14), ReviewResult.GOOD, strength = 70)

        val next = scheduler.scheduleNext(progress, ReviewResult.WEAK, today)

        assertEquals(QueueType.RECOVERY_WEAK, next.queueType)
        assertEquals(SchedulerStage.WEAK, next.stage)
        assertEquals(55, next.strength)
    }

    @Test
    fun `recovery requires configured consecutive strong reviews`() {
        val config = SchedulerConfig(recoveryStrongReviewsRequired = 2)
        val progress = Progress(1, QueueType.RECOVERY_WEAK, 0, today, today.minusDays(1), ReviewResult.WEAK)

        val first = scheduler.scheduleNext(progress, ReviewResult.STRONG, today, config)
        val second = scheduler.scheduleNext(first, ReviewResult.STRONG, today.plusDays(3), config)

        assertEquals(QueueType.RECOVERY_WEAK, first.queueType)
        assertEquals(QueueType.LONG_TERM, second.queueType)
        assertEquals(SchedulerStage.ROTATING, second.stage)
    }

    @Test
    fun `configured overload caps backlog`() {
        val config = SchedulerConfig(dailyCapacity = 20, maximumOverloadPercent = 125)
        val overdue = (1..50).map { Progress(it, QueueType.LONG_TERM, 0, today.minusDays(2), today.minusDays(20), ReviewResult.GOOD, dateMemorized = today.minusDays(it.toLong())) }

        val tasks = scheduler.getDailyTasks(overdue, today, overdue.size, config)

        assertEquals(25, tasks.size)
    }

    @Test
    fun `inactive and future pages are excluded`() {
        val inactive = Progress(1, QueueType.NEW, 0, today, null, null, active = false)
        val future = Progress(2, QueueType.NEW, 0, today.plusDays(1), null, null)

        assertEquals(emptyList<Progress>(), scheduler.getDailyTasks(listOf(inactive, future), today, 0))
    }

    @Test
    fun `review order is configurable`() {
        val old = Progress(1, QueueType.LONG_TERM, 0, today, today.minusDays(10), ReviewResult.GOOD, dateMemorized = today.minusDays(100))
        val new = Progress(2, QueueType.LONG_TERM, 0, today, today.minusDays(10), ReviewResult.GOOD, dateMemorized = today.minusDays(5))

        val tasks = scheduler.getDailyTasks(
            listOf(new, old), today, 2,
            SchedulerConfig(reviewOrder = SchedulerReviewOrder.OLDEST_FIRST)
        )

        assertEquals(listOf(1, 2), tasks.map { it.unitId })
    }
}
