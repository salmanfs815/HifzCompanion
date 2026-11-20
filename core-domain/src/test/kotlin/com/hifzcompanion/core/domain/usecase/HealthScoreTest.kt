package com.hifzcompanion.core.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HealthScoreTest {
    private val today = LocalDate.of(2026, 11, 20)
    private fun everyDay(days: Int) = (0 until days).map { today.minusDays(it.toLong()) }

    private fun health(
        memorized: Int = 100,
        overdue: List<Long> = emptyList(),
        weak: Int = 0,
        failed: Int = 0,
        reviews: List<LocalDate> = everyDay(30)
    ) = HealthBreakdown.calculate(memorized, overdue, weak, failed, reviews, today)

    @Test fun `on schedule, strong and consistent is 100`() {
        assertEquals(100, health().score)
    }

    @Test fun `no memorized pages and no reviews is 100`() {
        assertEquals(100, health(memorized = 0, reviews = emptyList()).score)
    }

    @Test fun `the same overdue share costs the same at any size`() {
        val small = health(memorized = 20, overdue = List(2) { 7L })
        val large = health(memorized = 600, overdue = List(60) { 7L })
        assertEquals(small.score, large.score)
        // 10% a week late against a 25% tolerance loses 40% of the 40 schedule points.
        assertEquals(84, small.score)
    }

    @Test fun `a page a day late costs less than one a week late`() {
        val slightly = health(overdue = List(10) { 1L })
        val badly = health(overdue = List(10) { 14L })
        assertEquals(98, slightly.score)
        assertEquals(84, badly.score)
    }

    @Test fun `failed pages count twice as much as weak ones`() {
        assertEquals(93, health(weak = 10).score)
        assertEquals(86, health(failed = 10).score)
        assertEquals(65, health(failed = 25).score)
    }

    @Test fun `consistency looks at the last 30 days`() {
        val everyOtherDay = (1 until 30 step 2).map { today.minusDays(it.toLong()) }
        val half = health(reviews = everyOtherDay)
        assertEquals(15, half.reviewDays)
        assertEquals(29, half.windowDays) // up to yesterday, since today has no review yet
        assertEquals(88, half.score)
        // A gap long ago no longer counts.
        assertEquals(100, health(reviews = everyDay(30) + today.minusDays(90)).score)
    }

    @Test fun `today only counts once reviewed`() {
        val upToYesterday = (1..30).map { today.minusDays(it.toLong()) }
        assertEquals(100, health(reviews = upToYesterday).score)
    }

    @Test fun `a new user is measured from their first review`() {
        val sinceThreeDays = health(reviews = everyDay(3))
        assertEquals(3, sinceThreeDays.windowDays)
        assertEquals(100, sinceThreeDays.score)
    }

    @Test fun `score never drops below zero`() {
        assertEquals(0, health(overdue = List(100) { 30L }, failed = 100, reviews = listOf(today.minusDays(40))).score)
    }
}
