package ca.salmanfs.hifzcompanion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    userSettingsRepository: UserSettingsRepository
) : ViewModel() {
    val isOnboardingCompleted: StateFlow<Boolean?> = userSettingsRepository.getUserSettings()
        .map { it.onboardingCompleted }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
