package com.hifzcompanion.core.domain.repository

import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.ReviewResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HifzRepository {
    fun getAllProgress(): Flow<List<Progress>>
    suspend fun updateProgress(progress: Progress)
    suspend fun recordReview(unitId: Int, result: ReviewResult, date: LocalDate)

    fun getTodayReviewCount(): Flow<Int>
}
