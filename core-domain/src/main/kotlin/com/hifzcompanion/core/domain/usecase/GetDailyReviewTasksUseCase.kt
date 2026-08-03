package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.scheduler.HifzScheduler
import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import com.hifzcompanion.core.domain.models.toSchedulerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

class GetDailyReviewTasksUseCase @Inject constructor(
    private val repository: HifzRepository,
    private val scheduler: HifzScheduler,
    private val settingsRepository: UserSettingsRepository
) {
    operator fun invoke(): Flow<List<Progress>> {
        val today = LocalDate.now()
        return combine(repository.getAllProgress(), settingsRepository.getUserSettings()) { allProgress, settings ->
            scheduler.getDailyTasks(
                allProgress = allProgress,
                today = today,
                memorizedPagesCount = allProgress.count { it.dateMemorized != null },
                config = settings.toSchedulerConfig()
            )
        }
    }
}
