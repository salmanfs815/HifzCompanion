package ca.salmanfs.hifzcompanion.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hifzcompanion.core.domain.usecase.DashboardStats
import com.hifzcompanion.core.domain.usecase.GetDashboardStatsUseCase
import com.hifzcompanion.notifications.ReviewNotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val stats: DashboardStats) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getDashboardStatsUseCase: GetDashboardStatsUseCase,
    notificationManager: ReviewNotificationManager
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = getDashboardStatsUseCase()
        .onEach { if (it.todayReviewCount == 0) notificationManager.dismissDailyReminders() }
        .map { DashboardUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )
}
