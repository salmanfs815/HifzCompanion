package com.hifzcompanion.core.data.repository

import com.hifzcompanion.core.data.dao.HifzDao
import com.hifzcompanion.core.data.entities.ReviewEventEntity
import com.hifzcompanion.core.data.entities.UserPageEntity
import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.QueueType
import com.hifzcompanion.scheduler.models.ReviewResult
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

    override suspend fun updateProgress(progress: Progress) {
        hifzDao.insertUserPage(progress.toEntity())
    }

    override suspend fun recordReview(unitId: Int, result: ReviewResult, date: LocalDate) {
        val timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        hifzDao.insertReviewEvent(
            ReviewEventEntity(
                pageId = unitId,
                timestamp = timestamp,
                result = result.name,
                difficulty = 0 // Placeholder
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
            currentStep = currentStep,
            nextReviewDate = Instant.ofEpochMilli(nextReviewDate).atZone(ZoneId.systemDefault()).toLocalDate(),
            lastReviewDate = lastReviewDate?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
            lastResult = lastResult?.let { ReviewResult.valueOf(it) }
        )
    }

    private fun Progress.toEntity(): UserPageEntity {
        return UserPageEntity(
            pageId = unitId,
            strength = 0, // Placeholder
            queueType = queueType.name,
            currentStep = currentStep,
            nextReviewDate = nextReviewDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            lastReviewDate = lastReviewDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            lastResult = lastResult?.name
        )
    }
}
