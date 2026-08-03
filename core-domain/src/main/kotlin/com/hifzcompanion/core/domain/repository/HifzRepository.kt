package com.hifzcompanion.core.domain.repository

import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.ReviewResult
import com.hifzcompanion.core.domain.models.ReviewRecord
import com.hifzcompanion.core.domain.models.ReviewType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HifzRepository {
    fun getAllProgress(): Flow<List<Progress>>
    fun getProgress(unitId: Int): Flow<Progress?>
    fun getAllReviewEvents(): Flow<List<ReviewRecord>>
    fun getReviewEvents(unitId: Int): Flow<List<ReviewRecord>>
    suspend fun updateProgress(progress: Progress)
    suspend fun recordReview(
        unitId: Int,
        result: ReviewResult,
        date: LocalDate,
        reviewType: ReviewType,
        durationSeconds: Int = 0,
        notes: String? = null
    )

    fun getTodayReviewCount(): Flow<Int>
}
