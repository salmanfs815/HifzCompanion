package com.hifzcompanion.core.data.repository

import com.hifzcompanion.core.data.dao.HifzDao
import com.hifzcompanion.core.data.entities.ReviewEventEntity
import com.hifzcompanion.core.data.entities.UserPageEntity
import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.QueueType
import com.hifzcompanion.scheduler.models.ReviewResult
import com.hifzcompanion.scheduler.models.SchedulerStage
import com.hifzcompanion.core.domain.models.ReviewRecord
import com.hifzcompanion.core.domain.models.ReviewType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class HifzRepositoryImpl @Inject constructor(
    private val hifzDao: HifzDao
) : HifzRepository {

    override fun getAllProgress(): Flow<List<Progress>> {
        return hifzDao.getAllUserPages().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getProgress(unitId: Int): Flow<Progress?> =
        hifzDao.getUserPage(unitId).map { it?.toDomain() }

    override fun getAllReviewEvents(): Flow<List<ReviewRecord>> =
        hifzDao.getAllReviewEvents().map { events -> events.map { it.toDomain() } }

    override fun getReviewEvents(unitId: Int): Flow<List<ReviewRecord>> =
        hifzDao.getReviewEventsForPage(unitId).map { events -> events.map { it.toDomain() } }

    override suspend fun updateProgress(progress: Progress) {
        hifzDao.insertUserPage(progress.toEntity())
    }

    override suspend fun recordReview(
        unitId: Int,
        result: ReviewResult,
        date: LocalDate,
        reviewType: ReviewType,
        durationSeconds: Int,
        notes: String?
    ) {
        val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        hifzDao.insertReviewEvent(
            ReviewEventEntity(
                pageId = unitId,
                timestamp = timestamp,
                result = result.name,
                difficulty = when (result) {
                    ReviewResult.STRONG -> 0
                    ReviewResult.GOOD -> 1
                    ReviewResult.WEAK -> 2
                    ReviewResult.FAILED -> 3
                },
                reviewType = reviewType.name,
                durationSeconds = durationSeconds,
                notes = notes
            )
        )
    }

    override fun getTodayReviewCount(): Flow<Int> {
        val startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return hifzDao.getReviewCountSince(startOfToday)
    }

    private fun UserPageEntity.toDomain(): Progress {
        return Progress(
            unitId = pageId,
            queueType = QueueType.valueOf(queueType),
            stage = runCatching { SchedulerStage.valueOf(stage) }.getOrDefault(SchedulerStage.NEW),
            currentStep = currentStep,
            nextReviewDate = Instant.ofEpochMilli(nextReviewDate).atZone(ZoneId.systemDefault()).toLocalDate(),
            lastReviewDate = lastReviewDate?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
            lastResult = lastResult?.let { ReviewResult.valueOf(it) },
            dateMemorized = dateMemorized?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
            strength = strength,
            consecutiveStrongReviews = consecutiveStrongReviews,
            active = active,
            notes = notes
        )
    }

    private fun Progress.toEntity(): UserPageEntity {
        return UserPageEntity(
            pageId = unitId,
            strength = strength,
            queueType = queueType.name,
            currentStep = currentStep,
            nextReviewDate = nextReviewDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            lastReviewDate = lastReviewDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            lastResult = lastResult?.name,
            stage = stage.name,
            dateMemorized = dateMemorized?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            consecutiveStrongReviews = consecutiveStrongReviews,
            active = active,
            notes = notes
        )
    }

    private fun ReviewEventEntity.toDomain() = ReviewRecord(
        id = id,
        pageId = pageId,
        timestamp = timestamp,
        quality = ReviewResult.valueOf(result),
        reviewType = runCatching { ReviewType.valueOf(reviewType) }.getOrDefault(ReviewType.SCHEDULED),
        durationSeconds = durationSeconds,
        notes = notes
    )
}
