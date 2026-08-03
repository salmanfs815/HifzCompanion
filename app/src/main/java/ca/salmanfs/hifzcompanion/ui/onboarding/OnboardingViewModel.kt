package ca.salmanfs.hifzcompanion.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hifzcompanion.backup.BackupManager
import com.hifzcompanion.backup.auth.GoogleAuthManager
import com.hifzcompanion.core.domain.models.MemorizationOrder
import com.hifzcompanion.core.domain.models.MushafType
import com.hifzcompanion.core.domain.models.UserSettings
import com.hifzcompanion.core.domain.usecase.InitializeUserProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context

data class OnboardingUiState(
    val currentStep: Int = 1,
    val mushafType: MushafType = MushafType.MADINAH,
    val memorizationOrder: MemorizationOrder = MemorizationOrder.BACK_TO_FRONT,
    val memorizedPages: List<Int> = emptyList(),
    val dailyTargetPages: Int = 1,
    val isAutoTarget: Boolean = true,
    val isOnboardingComplete: Boolean = false,
    val isBusy: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val initializeUserProgressUseCase: InitializeUserProgressUseCase,
    private val authManager: GoogleAuthManager,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun signIn(context: Context, serverClientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            try {
                authManager.authorize(context, serverClientId)
                nextStep()
            } catch (e: Exception) {
                _uiState.update { it.copy(message = e.message ?: "Google sign-in failed") }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun restoreFromCloud(context: Context, serverClientId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            try {
                val session = authManager.authorize(context, serverClientId)
                val result = backupManager.performRestore(session.accessToken)
                if (result.isSuccess) {
                    _uiState.update { it.copy(isOnboardingComplete = true) }
                } else {
                    _uiState.update { it.copy(message = result.exceptionOrNull()?.message ?: "Restore failed") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(message = e.message ?: "Restore failed") }
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    fun nextStep() {
        _uiState.update { it.copy(currentStep = it.currentStep + 1) }
    }

    fun previousStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(1)) }
    }

    fun setMushafType(type: MushafType) {
        _uiState.update { it.copy(mushafType = type) }
    }

    fun setMemorizationOrder(order: MemorizationOrder) {
        _uiState.update { it.copy(memorizationOrder = order) }
    }

    fun togglePage(pageId: Int) {
        _uiState.update { state ->
            val newList = if (state.memorizedPages.contains(pageId)) {
                state.memorizedPages - pageId
            } else {
                state.memorizedPages + pageId
            }
            state.withPages(newList)
        }
    }
    
    fun setMemorizedPages(pages: List<Int>) {
        _uiState.update { it.withPages(pages.filter { page -> page in 1..604 }.distinct().sorted()) }
    }

    fun setDailyTarget(pages: Int) {
        _uiState.update { it.copy(dailyTargetPages = pages, isAutoTarget = false) }
    }

    fun setAutoTarget(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isAutoTarget = enabled,
                dailyTargetPages = if (enabled) autoTarget(it.memorizedPages.size) else it.dailyTargetPages
            )
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val state = _uiState.value
            initializeUserProgressUseCase(
                settings = UserSettings(
                    mushafType = state.mushafType,
                    memorizationOrder = state.memorizationOrder,
                    dailyTargetPages = state.dailyTargetPages,
                    isAutoTarget = state.isAutoTarget
                ),
                memorizedPageIds = state.memorizedPages
            )
            _uiState.update { it.copy(isOnboardingComplete = true) }
        }
    }

    private fun OnboardingUiState.withPages(pages: List<Int>) = copy(
        memorizedPages = pages,
        dailyTargetPages = if (isAutoTarget) autoTarget(pages.size) else dailyTargetPages
    )

    private fun autoTarget(memorizedCount: Int) = maxOf(10, memorizedCount / 14).coerceAtMost(60)
}
