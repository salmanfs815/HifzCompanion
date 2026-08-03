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
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

data class OnboardingUiState(
    val currentStep: Int = 1,
    val mushafType: MushafType = MushafType.MADINAH,
    val memorizationOrder: MemorizationOrder = MemorizationOrder.BACK_TO_FRONT,
    val memorizedPages: List<Int> = emptyList(),
    val dailyTargetPages: Int = 1,
    val isAutoTarget: Boolean = true,
    val isOnboardingComplete: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val initializeUserProgressUseCase: InitializeUserProgressUseCase,
    private val authManager: GoogleAuthManager,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun signIn(context: Context) {
        viewModelScope.launch {
            try {
                // In a real app, use a real server client ID
                val response = authManager.signIn(context, "YOUR_SERVER_CLIENT_ID")
                val credential = GoogleIdTokenCredential.createFrom(response.credential.data)
                // Now authorize for Drive
                authManager.requestDriveAuthorization(context)
                
                // Assuming authorization succeeded, we can now check for backups
                // (Omitted for brevity in this step)
                nextStep()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun restoreFromCloud(context: Context) {
        viewModelScope.launch {
            try {
                // Assume we already authorized or re-authorize here
                // Get token (omitted) and restore
                val result = backupManager.performRestore("dummy_token")
                if (result.isSuccess) {
                    _uiState.update { it.copy(isOnboardingComplete = true) }
                }
            } catch (e: Exception) {
                // Handle error
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
            state.copy(memorizedPages = newList)
        }
    }
    
    fun setMemorizedPages(pages: List<Int>) {
        _uiState.update { it.copy(memorizedPages = pages) }
    }

    fun setDailyTarget(pages: Int) {
        _uiState.update { it.copy(dailyTargetPages = pages, isAutoTarget = false) }
    }

    fun setAutoTarget(enabled: Boolean) {
        _uiState.update { it.copy(isAutoTarget = enabled) }
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
}
