package com.hifzcompanion.core.domain.usecase

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Memorization health out of 100, made of three parts that scale with how much is memorized:
 *
 * - **On schedule** (40): how few pages are overdue. A page counts more the longer it is
 *   late, in full once a week overdue; all 40 points are gone when a quarter of the pages
 *   are a week or more overdue.
 * - **Recall** (35): how few pages were last rated Weak (half weight) or Failed (full
 *   weight); all 35 points are gone when a quarter of the pages are Failed.
 * - **Consistency** (25): days with a review over the last [WINDOW_DAYS] days, counted from
 *   the first review if that is more recent. Today only counts once it has a review.
 */
data class HealthBreakdown(
    val onSchedule: Double,
    val recall: Double,
    val consistency: Double,
    val overduePages: Int,
    val weakPages: Int,
    val failedPages: Int,
    val reviewDays: Int,
    val windowDays: Int
) {
    val score: Int = (onSchedule * SCHEDULE_POINTS + recall * RECALL_POINTS + consistency * CONSISTENCY_POINTS)
        .roundToInt().coerceIn(0, 100)

    companion object {
        const val SCHEDULE_POINTS = 40
        const val RECALL_POINTS = 35
        const val CONSISTENCY_POINTS = 25
        const val WINDOW_DAYS = 30

        /** Share of memorized pages at which a part's points are all lost. */
        private const val TOLERANCE = 0.25

        /** Days overdue at which a page counts in full against being on schedule. */
        private const val FULLY_LATE_DAYS = 7.0

        fun calculate(
            memorizedPages: Int,
            daysOverdue: List<Long>,
            weakPages: Int,
            failedPages: Int,
            reviewDates: Collection<LocalDate>,
            today: LocalDate
        ): HealthBreakdown {
            fun part(penalty: Double) =
                if (memorizedPages == 0) 1.0 else (1 - penalty / (memorizedPages * TOLERANCE)).coerceIn(0.0, 1.0)

            val lateness = daysOverdue.filter { it > 0 }.sumOf { (it / FULLY_LATE_DAYS).coerceAtMost(1.0) }
            val reviewed = reviewDates.filter { !it.isAfter(today) }.toSet()
            val first = reviewed.minOrNull()
            val end = if (today in reviewed) today else today.minusDays(1)
            val start = listOfNotNull(first, today.minusDays(WINDOW_DAYS - 1L)).max()
            val windowDays = if (first == null || end < start) 0 else ChronoUnit.DAYS.between(start, end).toInt() + 1
            val reviewDays = reviewed.count { it in start..end }

            return HealthBreakdown(
                onSchedule = part(lateness),
                recall = part(weakPages * 0.5 + failedPages),
                consistency = if (windowDays == 0) 1.0 else reviewDays.toDouble() / windowDays,
                overduePages = daysOverdue.count { it > 0 },
                weakPages = weakPages,
                failedPages = failedPages,
                reviewDays = reviewDays,
                windowDays = windowDays
            )
        }
    }
}
