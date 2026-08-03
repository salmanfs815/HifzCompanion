package ca.salmanfs.hifzcompanion.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hifzcompanion.core.domain.usecase.GetDailyReviewTasksUseCase
import com.hifzcompanion.core.domain.usecase.RecordReviewUseCase
import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.ReviewResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReviewUiState {
    data object Loading : ReviewUiState
    data class Session(
        val tasks: List<Progress>,
        val currentIndex: Int,
        val isFinished: Boolean = false
    ) : ReviewUiState {
        val currentPageProgress: Progress? = if (currentIndex < tasks.size) tasks[currentIndex] else null
    }
}

@HiltViewModel
class ReviewSessionViewModel @Inject constructor(
    private val getDailyReviewTasksUseCase: GetDailyReviewTasksUseCase,
    private val recordReviewUseCase: RecordReviewUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReviewUiState>(ReviewUiState.Loading)
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getDailyReviewTasksUseCase().collect { tasks ->
                if (_uiState.value is ReviewUiState.Loading) {
                    _uiState.value = ReviewUiState.Session(tasks, 0)
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
            }
        }
    }
}
