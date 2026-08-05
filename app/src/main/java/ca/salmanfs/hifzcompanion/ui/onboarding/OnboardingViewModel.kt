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
import android.app.PendingIntent
import android.content.Intent
import com.hifzcompanion.backup.auth.DriveAuthorizationResult
import com.hifzcompanion.backup.auth.GoogleSession
import com.hifzcompanion.backup.auth.BackupSessionStore

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
    private val backupManager: BackupManager,
    private val sessionStore: BackupSessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    private var pendingRestore = false

    fun connectGoogleDrive(
        context: Context,
        restore: Boolean,
        launchConsent: (PendingIntent) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null) }
            try {
                when (val authorization = authManager.beginDriveAuthorization(context)) {
                    is DriveAuthorizationResult.Granted -> finishGoogleDriveConnection(authorization.session, restore)
                    is DriveAuthorizationResult.RequiresConsent -> {
                        pendingRestore = restore
                        launchConsent(authorization.pendingIntent)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isBusy = false, message = e.message ?: "Google Drive connection failed") }
            }
        }
    }

    fun completeDriveAuthorization(data: Intent?) {
        viewModelScope.launch {
            if (data == null) {
                _uiState.update { it.copy(isBusy = false, message = "Google Drive connection was canceled.") }
                return@launch
            }
            try {
                finishGoogleDriveConnection(authManager.completeDriveAuthorization(data), pendingRestore)
            } catch (e: Exception) {
                _uiState.update { it.copy(isBusy = false, message = e.message ?: "Google Drive connection failed") }
            }
        }
    }

    private suspend fun finishGoogleDriveConnection(session: GoogleSession, restore: Boolean) {
        sessionStore.saveAccessToken(session.accessToken)
        if (!restore) {
            _uiState.update { it.copy(isBusy = false) }
            nextStep()
            return
        }
        val result = backupManager.performRestore(session.accessToken)
        if (result.isSuccess) {
            _uiState.update { it.copy(isBusy = false, isOnboardingComplete = true) }
        } else {
            _uiState.update { it.copy(isBusy = false, message = result.exceptionOrNull()?.message ?: "Restore failed") }
        }
    }

    fun nextStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(4)) }
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
