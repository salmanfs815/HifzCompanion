package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.models.*
import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import com.hifzcompanion.scheduler.HifzScheduler
import com.hifzcompanion.scheduler.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class LogManualReviewUseCaseTest {
    @Test
    fun manualReviewReschedulesDuePageAndRecordsManualHistory() = runTest {
        val today = LocalDate.now()
        val repository = FakeHifzRepository(
            Progress(10, QueueType.LONG_TERM, 0, today, today.minusDays(14), ReviewResult.GOOD, dateMemorized = today.minusYears(1))
        )
        val useCase = LogManualReviewUseCase(repository, FakeSettingsRepository(), HifzScheduler())

        useCase(listOf(10), ReviewResult.STRONG, newlyMemorized = false)

        assertTrue(repository.progress.value.getValue(10).nextReviewDate > today)
        assertEquals(ReviewType.MANUAL, repository.events.single().reviewType)
    }

    @Test
    fun newlyMemorizedPageEntersTrackedSchedule() = runTest {
        val repository = FakeHifzRepository()
        val useCase = LogManualReviewUseCase(repository, FakeSettingsRepository(), HifzScheduler())

        useCase(listOf(604), ReviewResult.GOOD, newlyMemorized = true)

        val saved = repository.progress.value.getValue(604)
        assertTrue(saved.active)
        assertNotNull(saved.dateMemorized)
        assertTrue(saved.nextReviewDate > LocalDate.now())
    }

    @Test
    fun correctingMemorizationReplacesStaleNextPageSuggestion() = runTest {
        val today = LocalDate.now()
        val staleSuggestion = Progress(50, QueueType.NEW, 0, today, null, null)
        val repository = FakeHifzRepository(staleSuggestion)
        val useCase = UpdateMemorizationStatusUseCase(repository, FakeSettingsRepository())

        useCase((100..110).toList())

        assertFalse(repository.progress.value.getValue(50).active)
        val correctedSuggestion = repository.progress.value.getValue(99)
        assertTrue(correctedSuggestion.active)
        assertEquals(QueueType.NEW, correctedSuggestion.queueType)
        assertNull(correctedSuggestion.dateMemorized)
    }

    @Test
    fun removingMemorizedPageDeletesItsProgressAndHistory() = runTest {
        val today = LocalDate.now()
        val repository = FakeHifzRepository(
            Progress(10, QueueType.LONG_TERM, 0, today, today.minusDays(1), ReviewResult.GOOD, dateMemorized = today.minusYears(1))
        )
        repository.events += ReviewRecord(1, 10, 0, ReviewResult.GOOD, ReviewType.SCHEDULED)

        UpdateMemorizationStatusUseCase(repository, FakeSettingsRepository())(emptyList())

        assertFalse(repository.progress.value.containsKey(10))
        assertTrue(repository.events.none { it.pageId == 10 })
    }

    @Test
    fun correctingTodaysStrengthReplaysSchedulerState() = runTest {
        val today = LocalDate.now()
        val repository = FakeHifzRepository(
            Progress(25, QueueType.LONG_TERM, 0, today.plusDays(28), today, ReviewResult.STRONG, dateMemorized = today.minusYears(1))
        )
        val event = ReviewRecord(
            id = 1,
            pageId = 25,
            timestamp = today.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            quality = ReviewResult.STRONG,
            reviewType = ReviewType.SCHEDULED
        )
        repository.events += event

        EditReviewEventUseCase(repository, FakeSettingsRepository(), HifzScheduler())(event, ReviewResult.WEAK)

        assertEquals(ReviewResult.WEAK, repository.events.single().quality)
        assertEquals(QueueType.RECOVERY_WEAK, repository.progress.value.getValue(25).queueType)
        assertEquals(today.plusDays(1), repository.progress.value.getValue(25).nextReviewDate)
    }
}

private class FakeSettingsRepository(private val settings: UserSettings = UserSettings()) : UserSettingsRepository {
    override fun getUserSettings(): Flow<UserSettings> = flowOf(settings)
    override suspend fun updateUserSettings(settings: UserSettings) = Unit
}

private class FakeHifzRepository(vararg initial: Progress) : HifzRepository {
    val progress = MutableStateFlow(initial.associateBy { it.unitId })
    val events = mutableListOf<ReviewRecord>()

    override fun getAllProgress(): Flow<List<Progress>> = progress.map { it.values.toList() }
    override fun getProgress(unitId: Int): Flow<Progress?> = progress.map { it[unitId] }
    override fun getAllReviewEvents(): Flow<List<ReviewRecord>> = flowOf(events)
    override fun getReviewEvents(unitId: Int): Flow<List<ReviewRecord>> = flowOf(events.filter { it.pageId == unitId })
    override suspend fun getReviewEventsSnapshot(unitId: Int): List<ReviewRecord> = events.filter { it.pageId == unitId }.sortedWith(compareBy({ it.timestamp }, { it.id }))
    override suspend fun updateReviewEventQuality(eventId: Int, result: ReviewResult) {
        val index = events.indexOfFirst { it.id == eventId }
        if (index >= 0) events[index] = events[index].copy(quality = result)
    }
    override suspend fun updateProgress(progress: Progress) { this.progress.update { it + (progress.unitId to progress) } }
    override suspend fun recordReview(unitId: Int, result: ReviewResult, date: LocalDate, reviewType: ReviewType, durationSeconds: Int, notes: String?) {
        events += ReviewRecord(events.size + 1, unitId, 0, result, reviewType, durationSeconds, notes)
    }
    override fun getReviewCountForDate(date: LocalDate): Flow<Int> = flowOf(0)
    override suspend fun deleteProgressAndHistory(unitId: Int) {
        progress.update { it - unitId }
        events.removeAll { it.pageId == unitId }
    }
}
