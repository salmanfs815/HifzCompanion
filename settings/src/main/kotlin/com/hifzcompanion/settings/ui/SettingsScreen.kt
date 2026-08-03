package com.hifzcompanion.settings.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hifzcompanion.backup.BackupManager
import com.hifzcompanion.backup.auth.BackupSessionStore
import com.hifzcompanion.backup.auth.GoogleAuthManager
import com.hifzcompanion.backup.worker.BackupWorker
import com.hifzcompanion.core.domain.models.AppTheme
import com.hifzcompanion.core.domain.models.UserSettings
import com.hifzcompanion.core.domain.models.ReviewOrder
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val isBusy: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserSettingsRepository,
    private val authManager: GoogleAuthManager,
    private val sessionStore: BackupSessionStore,
    private val backupManager: BackupManager
) : ViewModel() {
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    val state = combine(repository.getUserSettings(), busy, message, ::SettingsUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun update(transform: (UserSettings) -> UserSettings) = viewModelScope.launch {
        repository.updateUserSettings(transform(state.value.settings))
    }

    fun backup(context: Context, clientId: String) = runBackupAction(context, clientId, restore = false)
    fun restore(context: Context, clientId: String) = runBackupAction(context, clientId, restore = true)

    private fun runBackupAction(context: Context, clientId: String, restore: Boolean) = viewModelScope.launch {
        busy.value = true
        message.value = null
        try {
            val session = authManager.authorize(context, clientId)
            sessionStore.saveAccessToken(session.accessToken)
            val result = if (restore) backupManager.performRestore(session.accessToken) else backupManager.performBackup(session.accessToken)
            message.value = if (result.isSuccess) {
                if (restore) "Restore complete" else "Backup complete"
            } else result.exceptionOrNull()?.message ?: "Operation failed"
            if (!restore && state.value.settings.automaticBackupEnabled) BackupWorker.schedule(context)
        } catch (error: Exception) {
            message.value = error.message ?: "Google Drive operation failed"
        } finally {
            busy.value = false
        }
    }

    fun setAutomaticBackup(context: Context, enabled: Boolean) {
        update { it.copy(automaticBackupEnabled = enabled) }
        if (enabled && sessionStore.getAccessToken() != null) BackupWorker.schedule(context) else BackupWorker.cancel(context)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    serverClientId: String,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val settings = state.settings
    val context = LocalContext.current
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).widthIn(max = 720.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Scheduler", style = MaterialTheme.typography.titleLarge)
            SettingSlider("Daily capacity", settings.dailyCapacity, 10..60) { viewModel.update { s -> s.copy(dailyCapacity = it) } }
            SettingSlider("Minimum pages", settings.minimumPages, 1..30) { viewModel.update { s -> s.copy(minimumPages = it) } }
            SettingSlider("Rotation days", settings.rotationDays, 7..60) { viewModel.update { s -> s.copy(rotationDays = it) } }
            SettingSlider("Maximum overload", settings.maximumOverloadPercent, 100..200, "%") { viewModel.update { s -> s.copy(maximumOverloadPercent = it) } }
            SettingSlider("Strong reviews to exit recovery", settings.recoveryStrongReviewsRequired, 1..5) { viewModel.update { s -> s.copy(recoveryStrongReviewsRequired = it) } }
            Text("Review order")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ReviewOrder.entries.forEachIndexed { index, order ->
                    SegmentedButton(
                        selected = settings.reviewOrder == order,
                        onClick = { viewModel.update { it.copy(reviewOrder = order) } },
                        shape = SegmentedButtonDefaults.itemShape(index, ReviewOrder.entries.size)
                    ) { Text(order.name.substringBefore('_').lowercase().replaceFirstChar(Char::uppercase)) }
                }
            }
            SwitchSetting("Auto-advance after rating", settings.autoAdvance) { viewModel.update { s -> s.copy(autoAdvance = it) } }
            ScheduleField("New schedule", settings.newSchedule) { viewModel.update { s -> s.copy(newSchedule = it) } }
            ScheduleField("Weak schedule", settings.weakSchedule) { viewModel.update { s -> s.copy(weakSchedule = it) } }
            ScheduleField("Failed schedule", settings.failedSchedule) { viewModel.update { s -> s.copy(failedSchedule = it) } }

            HorizontalDivider()
            Text("Notifications", style = MaterialTheme.typography.titleLarge)
            SwitchSetting("Enable reminders", settings.notificationsEnabled) { viewModel.update { s -> s.copy(notificationsEnabled = it) } }
            TimeField("Morning", settings.morningNotificationTime) { viewModel.update { s -> s.copy(morningNotificationTime = it) } }
            TimeField("Evening", settings.eveningNotificationTime) { viewModel.update { s -> s.copy(eveningNotificationTime = it) } }
            SettingSlider("Weekly summary day (1=Mon)", settings.weeklyNotificationDay, 1..7) { viewModel.update { s -> s.copy(weeklyNotificationDay = it) } }

            HorizontalDivider()
            Text("Appearance", style = MaterialTheme.typography.titleLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                AppTheme.entries.forEachIndexed { index, theme ->
                    SegmentedButton(
                        selected = settings.theme == theme,
                        onClick = { viewModel.update { it.copy(theme = theme) } },
                        shape = SegmentedButtonDefaults.itemShape(index, AppTheme.entries.size)
                    ) { Text(theme.name.lowercase().replaceFirstChar(Char::uppercase)) }
                }
            }

            HorizontalDivider()
            Text("Backup & restore", style = MaterialTheme.typography.titleLarge)
            SwitchSetting("Automatic daily backup", settings.automaticBackupEnabled) { viewModel.setAutomaticBackup(context, it) }
            Button(onClick = { viewModel.backup(context, serverClientId) }, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) { Text("Back up to Google Drive") }
            OutlinedButton(onClick = { viewModel.restore(context, serverClientId) }, enabled = !state.isBusy, modifier = Modifier.fillMaxWidth()) { Text("Restore from Google Drive") }
            if (state.isBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable private fun SettingSlider(label: String, value: Int, range: IntRange, suffix: String = "", onChange: (Int) -> Unit) {
    Column { Text("$label: $value$suffix"); Slider(value.toFloat(), { onChange(it.toInt()) }, valueRange = range.first.toFloat()..range.last.toFloat()) }
}

@Composable private fun SwitchSetting(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, onChange) }
}

@Composable private fun ScheduleField(label: String, values: List<Int>, onChange: (List<Int>) -> Unit) {
    OutlinedTextField(
        value = values.joinToString(","),
        onValueChange = { text -> text.split(',').mapNotNull(String::trim).mapNotNull(String::toIntOrNull).takeIf(List<Int>::isNotEmpty)?.let(onChange) },
        label = { Text("$label (days)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
    )
}

@Composable private fun TimeField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text("$label time (HH:mm)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}
