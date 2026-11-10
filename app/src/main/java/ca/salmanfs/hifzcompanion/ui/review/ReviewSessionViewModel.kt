package ca.salmanfs.hifzcompanion.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hifzcompanion.core.domain.models.SessionSortOrder
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import com.hifzcompanion.core.domain.usecase.GetDailyReviewTasksUseCase
import com.hifzcompanion.core.domain.usecase.inSessionOrder
import com.hifzcompanion.core.domain.usecase.RecordReviewUseCase
import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.ReviewResult
import com.hifzcompanion.notifications.ReviewNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface ReviewUiState {
    data object Loading : ReviewUiState
    data class Session(
        val tasks: List<Progress>,
        val currentIndex: Int,
        val isFinished: Boolean = false,
        val sortOrder: SessionSortOrder = SessionSortOrder.PRIORITY,
        val today: LocalDate = LocalDate.now()
    ) : ReviewUiState {
        val currentPageProgress: Progress? = if (currentIndex < tasks.size) tasks[currentIndex] else null

        /** Days until the current page is actually due; > 0 means it was pulled forward to meet the daily minimum. */
        val currentDaysAhead: Long = currentPageProgress
            ?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it.nextReviewDate) }
            ?.coerceAtLeast(0) ?: 0
    }
}

@HiltViewModel
class ReviewSessionViewModel @Inject constructor(
    private val getDailyReviewTasksUseCase: GetDailyReviewTasksUseCase,
    private val recordReviewUseCase: RecordReviewUseCase,
    private val notificationManager: ReviewNotificationManager,
    private val settingsRepository: UserSettingsRepository
) : ViewModel() {

    /** Today's tasks as the scheduler prioritised them, used to restore PRIORITY order. */
    private var priorityTasks: List<Progress> = emptyList()

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getDailyReviewTasksUseCase().collect { tasks ->
                if (_uiState.value is ReviewUiState.Loading) {
                    val order = settingsRepository.getUserSettings().first().sessionSortOrder
                    priorityTasks = tasks
                    _uiState.value = ReviewUiState.Session(
                        tasks = tasks.inSessionOrder(order),
                        currentIndex = 0,
                        isFinished = tasks.isEmpty(),
                        sortOrder = order
                    )
                    if (tasks.isEmpty()) notificationManager.dismissDailyReminders()
                }
            }
        }
    }

    fun submitRating(result: ReviewResult) {
        val currentState = _uiState.value as? ReviewUiState.Session ?: return
        val currentProgress = currentState.currentPageProgress ?: return

        viewModelScope.launch {
            recordReviewUseCase(currentProgress, result)
            
            val nextIndex = currentState.currentIndex + 1
            if (nextIndex < currentState.tasks.size) {
                _uiState.value = currentState.copy(currentIndex = nextIndex)
            } else {
                _uiState.value = currentState.copy(currentIndex = nextIndex, isFinished = true)
                notificationManager.dismissDailyReminders()
            }
        }
    }

    fun skipCurrentPage() {
        val currentState = _uiState.value as? ReviewUiState.Session ?: return
        if (currentState.isFinished || currentState.currentIndex >= currentState.tasks.lastIndex) return
        val current = currentState.tasks[currentState.currentIndex]
        val reordered = currentState.tasks.toMutableList().apply {
            removeAt(currentState.currentIndex)
            add(current)
        }
        _uiState.value = currentState.copy(tasks = reordered)
    }

    /** Re-orders only the pages not yet reviewed and remembers the choice. */
    fun setSortOrder(order: SessionSortOrder) {
        val currentState = _uiState.value as? ReviewUiState.Session ?: return
        if (currentState.sortOrder == order) return
        val reviewed = currentState.tasks.take(currentState.currentIndex)
        val remainingIds = currentState.tasks.drop(currentState.currentIndex).map { it.unitId }.toSet()
        val remaining = priorityTasks.filter { it.unitId in remainingIds }.inSessionOrder(order)
        _uiState.value = currentState.copy(tasks = reviewed + remaining, sortOrder = order)
        viewModelScope.launch {
            val settings = settingsRepository.getUserSettings().first()
            settingsRepository.updateUserSettings(settings.copy(sessionSortOrder = order))
        }
    }
}
