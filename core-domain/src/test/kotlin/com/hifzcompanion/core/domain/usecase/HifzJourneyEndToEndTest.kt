package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.models.MemorizationOrder
import com.hifzcompanion.core.domain.models.ReviewRecord
import com.hifzcompanion.core.domain.models.ReviewType
import com.hifzcompanion.core.domain.models.UserSettings
import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import com.hifzcompanion.scheduler.HifzScheduler
import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.QueueType
import com.hifzcompanion.scheduler.models.ReviewResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Covers the primary offline journey across onboarding, queue generation, and review recording. */
class HifzJourneyEndToEndTest {
    @Test
    fun onboardingToReviewUpdatesQueuesAndHistory() = runTest {
        val repository = JourneyRepository()
        val settingsRepository = JourneySettingsRepository()
        val scheduler = HifzScheduler()
        val settings = UserSettings(
            memorizationOrder = MemorizationOrder.FRONT_TO_BACK,
            dailyCapacity = 10,
            minimumPages = 1
        )

        InitializeUserProgressUseCase(repository, settingsRepository)(settings, listOf(1, 2))

        assertTrue(settingsRepository.settings.value.onboardingCompleted)
        assertEquals(QueueType.LONG_TERM, repository.progress.value.getValue(1).queueType)
        assertEquals(QueueType.NEW, repository.progress.value.getValue(3).queueType)

        val beforeReview = GetDailyReviewTasksUseCase(repository, scheduler, settingsRepository)().first()
        assertEquals(listOf(3, 1, 2), beforeReview.map { it.unitId })

        RecordReviewUseCase(repository, scheduler, settingsRepository)(
            repository.progress.value.getValue(1),
            ReviewResult.WEAK,
            durationSeconds = 90,
            notes = "Needs another pass"
        )

        val reviewed = repository.progress.value.getValue(1)
        assertEquals(QueueType.RECOVERY_WEAK, reviewed.queueType)
        assertEquals(ReviewType.SCHEDULED, repository.events.single().reviewType)
        assertEquals(90, repository.events.single().durationSeconds)
        assertFalse(GetDailyReviewTasksUseCase(repository, scheduler, settingsRepository)().first().any { it.unitId == 1 })
        assertEquals(
            1,
            GetDashboardStatsUseCase(repository, scheduler, settingsRepository)().first().recoveryCount
        )
    }
}

private class JourneySettingsRepository : UserSettingsRepository {
    val settings = MutableStateFlow(UserSettings())
    override fun getUserSettings(): Flow<UserSettings> = settings
    override suspend fun updateUserSettings(settings: UserSettings) { this.settings.value = settings }
}

private class JourneyRepository : HifzRepository {
    val progress = MutableStateFlow<Map<Int, Progress>>(emptyMap())
    val events = mutableListOf<ReviewRecord>()

    override fun getAllProgress(): Flow<List<Progress>> = progress.map { it.values.toList() }
    override fun getProgress(unitId: Int): Flow<Progress?> = progress.map { it[unitId] }
    override fun getAllReviewEvents(): Flow<List<ReviewRecord>> = MutableStateFlow(events)
    override fun getReviewEvents(unitId: Int): Flow<List<ReviewRecord>> = MutableStateFlow(events.filter { it.pageId == unitId })
    override suspend fun getReviewEventsSnapshot(unitId: Int): List<ReviewRecord> = events.filter { it.pageId == unitId }
    override suspend fun updateReviewEventQuality(eventId: Int, result: ReviewResult) = Unit
    override suspend fun updateProgress(progress: Progress) { this.progress.update { it + (progress.unitId to progress) } }
    override suspend fun recordReview(
        unitId: Int,
        result: ReviewResult,
        date: LocalDate,
        reviewType: ReviewType,
        durationSeconds: Int,
        notes: String?
    ) {
        events += ReviewRecord(
            id = events.size + 1,
            pageId = unitId,
            timestamp = 0,
            quality = result,
            reviewType = reviewType,
            durationSeconds = durationSeconds,
            notes = notes
        )
    }

    override fun getTodayReviewCount(): Flow<Int> = MutableStateFlow(events.size)
}
