package com.hifzcompanion.core.domain.usecase

import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.scheduler.HifzScheduler
import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import com.hifzcompanion.core.domain.models.toSchedulerConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

class GetDailyReviewTasksUseCase @Inject constructor(
    private val repository: HifzRepository,
    private val scheduler: HifzScheduler,
    private val settingsRepository: UserSettingsRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Progress>> {
        return systemDateFlow().flatMapLatest { today ->
            combine(
                repository.getAllProgress(),
                settingsRepository.getUserSettings(),
                repository.getReviewCountForDate(today)
            ) { allProgress, settings, completedToday ->
                val config = settings.toSchedulerConfig()
                val remainingAllowance = (scheduler.maximumDailyTasks(config) - completedToday).coerceAtLeast(0)
                scheduler.getDailyTasks(
                    allProgress = allProgress,
                    today = today,
                    memorizedPagesCount = allProgress.count { it.active && it.dateMemorized != null },
                    config = config
                ).take(remainingAllowance)
            }
        }
    }
}
