package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.scheduler.HifzScheduler
import com.hifzcompanion.scheduler.models.Progress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class GetDailyReviewTasksUseCase @Inject constructor(
    private val repository: HifzRepository,
    private val scheduler: HifzScheduler
) {
    operator fun invoke(): Flow<List<Progress>> {
        val today = LocalDate.now()
        return repository.getAllProgress().map { allProgress ->
            scheduler.getDailyTasks(
                allProgress = allProgress,
                today = today,
                memorizedPagesCount = allProgress.count { it.lastReviewDate != null }
            )
        }
    }
}
