package com.hifzcompanion.settings.ui

import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.hifzcompanion.backup.auth.DriveAuthorizationResult
import com.hifzcompanion.backup.auth.GoogleSession
import com.hifzcompanion.backup.worker.BackupWorker
import com.hifzcompanion.core.domain.models.AppTheme
import com.hifzcompanion.core.domain.models.UserSettings
import com.hifzcompanion.core.domain.models.ReviewOrder
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalTime
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val isBusy: Boolean = false,
    val message: String? = null,
    val lastSuccessfulBackup: Long? = null
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
    val state = combine(repository.getUserSettings(), busy, message, sessionStore.lastSuccessfulBackup) {
            settings, isBusy, status, lastBackup ->
        SettingsUiState(settings, isBusy, status, lastBackup)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun update(transform: (UserSettings) -> UserSettings) = viewModelScope.launch {
        repository.updateUserSettings(transform(state.value.settings))
    }

    private var pendingRestore = false

    fun backup(context: Context, launchConsent: (PendingIntent) -> Unit) = beginBackupAction(context, restore = false, launchConsent)
    fun restore(context: Context, launchConsent: (PendingIntent) -> Unit) = beginBackupAction(context, restore = true, launchConsent)

    private fun beginBackupAction(context: Context, restore: Boolean, launchConsent: (PendingIntent) -> Unit) = viewModelScope.launch {
        busy.value = true
        message.value = null
        try {
            when (val authorization = authManager.beginDriveAuthorization(context)) {
                is DriveAuthorizationResult.Granted -> performBackupAction(context, authorization.session, restore)
                is DriveAuthorizationResult.RequiresConsent -> {
                    pendingRestore = restore
                    launchConsent(authorization.pendingIntent)
                }
            }
        } catch (error: Exception) {
            message.value = error.message ?: "Google Drive operation failed"
            busy.value = false
        }
    }

    fun completeAuthorization(context: Context, data: Intent?) = viewModelScope.launch {
        if (data == null) {
            message.value = "Google Drive connection was canceled."
            busy.value = false
            return@launch
        }
        try {
            performBackupAction(context, authManager.completeDriveAuthorization(data), pendingRestore)
        } catch (error: Exception) {
            message.value = error.message ?: "Google Drive authorization failed"
            busy.value = false
        }
    }

    private suspend fun performBackupAction(context: Context, session: GoogleSession, restore: Boolean) {
        sessionStore.saveAccessToken(session.accessToken)
        val result = if (restore) {
            backupManager.performRestore(session.accessToken)
        } else {
            backupManager.performBackup(session.accessToken)
        }
        message.value = if (result.isSuccess) {
            if (restore) "Restore complete" else "Backup complete"
        } else result.exceptionOrNull()?.message ?: "Operation failed"
        if (!restore && result.isSuccess) sessionStore.recordSuccessfulBackup()
        if (!restore && result.isSuccess && state.value.settings.automaticBackupEnabled) BackupWorker.schedule(context)
        busy.value = false
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
    onEditMemorization: () -> Unit,
    launchAuthorization: (PendingIntent) -> Unit,
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
            Text("Memorization", style = MaterialTheme.typography.titleLarge)
            Text("Correct pages, Surahs, or Juz selected during onboarding.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onEditMemorization, modifier = Modifier.fillMaxWidth()) {
                Text("Update current memorization")
            }

            HorizontalDivider()
            Text("Scheduler", style = MaterialTheme.typography.titleLarge)
            SettingSlider("Daily capacity", "Usual maximum number of review pages assigned per day.", settings.dailyCapacity, 10..60) { viewModel.update { s -> s.copy(dailyCapacity = it) } }
            SettingSlider("Minimum pages", "On light days, pages due soonest are pulled forward to reach this. Skipping them never makes them overdue.", settings.minimumPages, 1..30) { viewModel.update { s -> s.copy(minimumPages = it) } }
            SettingSlider("Rotation days", "Number of days in which the app aims to cycle through memorized pages.", settings.rotationDays, 7..60) { viewModel.update { s -> s.copy(rotationDays = it) } }
            SettingSlider("Maximum overload", "Allows urgent recovery and backlog work to exceed daily capacity.", settings.maximumOverloadPercent, 100..200, "%") { viewModel.update { s -> s.copy(maximumOverloadPercent = it) } }
            SettingSlider("Strong reviews to exit recovery", "Consecutive Strong ratings needed before a weak page returns to rotation.", settings.recoveryStrongReviewsRequired, 1..5) { viewModel.update { s -> s.copy(recoveryStrongReviewsRequired = it) } }
            Text("Review order")
            Text("Controls how long-term pages are prioritized when more pages are due than fit today.", style = MaterialTheme.typography.bodySmall)
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
            Text("Move automatically to the next page after choosing a strength.", style = MaterialTheme.typography.bodySmall)
            ScheduleField("New schedule", "Day intervals used while newly memorized pages stabilize.", settings.newSchedule) { viewModel.update { s -> s.copy(newSchedule = it) } }
            ScheduleField("Weak schedule", "Day intervals used to rebuild pages rated Weak.", settings.weakSchedule) { viewModel.update { s -> s.copy(weakSchedule = it) } }
            ScheduleField("Failed schedule", "Day intervals used after a Failed recitation.", settings.failedSchedule) { viewModel.update { s -> s.copy(failedSchedule = it) } }

            HorizontalDivider()
            Text("Notifications", style = MaterialTheme.typography.titleLarge)
            SwitchSetting("Enable reminders", settings.notificationsEnabled) { viewModel.update { s -> s.copy(notificationsEnabled = it) } }
            TimeField("Morning", settings.morningNotificationTime) { viewModel.update { s -> s.copy(morningNotificationTime = it) } }
            TimeField("Evening", settings.eveningNotificationTime) { viewModel.update { s -> s.copy(eveningNotificationTime = it) } }
            WeeklyDaySlider(settings.weeklyNotificationDay) { viewModel.update { s -> s.copy(weeklyNotificationDay = it) } }

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
            Text(
                state.lastSuccessfulBackup?.let { timestamp ->
                    "Last successful backup: " + java.time.Instant.ofEpochMilli(timestamp)
                        .atZone(java.time.ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.getDefault()))
                } ?: "Last successful backup: Never",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SwitchSetting("Automatic daily backup", settings.automaticBackupEnabled) { viewModel.setAutomaticBackup(context, it) }
            Button(
                onClick = { viewModel.backup(context, launchAuthorization) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Back up to Google Drive") }
            OutlinedButton(
                onClick = { viewModel.restore(context, launchAuthorization) },
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Restore from Google Drive") }
            if (state.isBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable private fun SettingSlider(label: String, description: String, value: Int, range: IntRange, suffix: String = "", onChange: (Int) -> Unit) {
    Column {
        Text("$label: $value$suffix")
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(value.toFloat(), { onChange(it.toInt()) }, valueRange = range.first.toFloat()..range.last.toFloat())
    }
}

@Composable private fun SwitchSetting(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, onChange) }
}

@Composable private fun WeeklyDaySlider(isoDay: Int, onChange: (Int) -> Unit) {
    val sundayFirst = remember {
        listOf(
            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY
        )
    }
    val selectedIndex = sundayFirst.indexOf(DayOfWeek.of(isoDay.coerceIn(1, 7))).coerceAtLeast(0)
    Column {
        Text("Weekly summary day: ${sundayFirst[selectedIndex].displayName()}")
        Text("Day of week for the weekly progress summary.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = selectedIndex.toFloat(),
            onValueChange = { position -> onChange(sundayFirst[position.toInt().coerceIn(0, 6)].value) },
            valueRange = 0f..6f,
            steps = 5
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Sunday", style = MaterialTheme.typography.labelSmall)
            Text("Saturday", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun DayOfWeek.displayName(): String = name.lowercase().replaceFirstChar(Char::uppercase)

@Composable private fun ScheduleField(label: String, description: String, values: List<Int>, onChange: (List<Int>) -> Unit) {
    Column {
        OutlinedTextField(
            value = values.joinToString(","),
            onValueChange = { text -> text.split(',').mapNotNull(String::trim).mapNotNull(String::toIntOrNull).takeIf(List<Int>::isNotEmpty)?.let(onChange) },
            label = { Text("$label (days)") }, modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TimeField(label: String, value: String, onChange: (String) -> Unit) {
    val parsed = remember(value) { runCatching { LocalTime.parse(value) }.getOrDefault(LocalTime.NOON) }
    var showPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val display = remember(parsed) { parsed.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())) }
    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Text("$label reminder", style = MaterialTheme.typography.labelSmall)
            Text(display, style = MaterialTheme.typography.titleMedium)
        }
    }
    if (showPicker) {
        val pickerState = rememberTimePickerState(initialHour = parsed.hour, initialMinute = parsed.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Select $label time") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    onChange("%02d:%02d".format(Locale.US, pickerState.hour, pickerState.minute))
                    showPicker = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        )
    }
}
