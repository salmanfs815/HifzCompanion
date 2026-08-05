package ca.salmanfs.hifzcompanion.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hifzcompanion.core.domain.usecase.DashboardStats
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToReview: () -> Unit,
    onNavigateToManualLog: () -> Unit,
    onNavigateToActivityLog: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToQueue: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSize = LocalWindowInfo.current.containerSize
    val isExpanded = with(LocalDensity.current) { windowSize.width.toDp() >= 840.dp }
    var showHelp by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    IconButton(onClick = onNavigateToActivityLog) { Icon(Icons.Default.History, "Today's activity") }
                    IconButton(onClick = onNavigateToStatistics) { Icon(Icons.Default.BarChart, "Statistics") }
                    IconButton(onClick = { showHelp = true }) { Icon(Icons.AutoMirrored.Filled.HelpOutline, "Help") }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Settings") }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is DashboardUiState.Success -> {
                DashboardContent(
                    stats = state.stats,
                    isExpanded = isExpanded,
                    onNavigateToReview = onNavigateToReview,
                    onNavigateToManualLog = onNavigateToManualLog,
                    onNavigateToStatistics = onNavigateToStatistics,
                    onNavigateToQueue = onNavigateToQueue,
                    modifier = Modifier.padding(padding)
                )
            }
            is DashboardUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message)
                }
            }
        }
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("How to use Hifz Companion") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Today's Review contains pages due from your rotation. Start the session and rate each recitation honestly.")
                    Text("New Memorization stabilizes recently memorized pages. Recovery gives extra attention to Weak or Failed pages.")
                    Text("Use Log memorization or extra review whenever you revise outside the suggested schedule.")
                    Text("Use the history icon to review today's activity and correct an accidental rating. Settings lets you adjust memorized pages, schedules, reminders, and backup.")
                }
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Got it") } }
        )
    }
}

@Composable
fun DashboardContent(
    stats: DashboardStats,
    isExpanded: Boolean,
    onNavigateToReview: () -> Unit,
    onNavigateToManualLog: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToQueue: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TodayProgressCard(stats.todayProgressPages, stats.totalProgressPages)

        val columns = if (isExpanded) 3 else 2
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            item {
                StatCard("Today's Review", stats.todayReviewCount.toString(), Icons.Default.Refresh) { onNavigateToQueue("DUE") }
            }
            item {
                StatCard("New Memorization", stats.newMemorizationCount.toString(), Icons.Default.Add) { onNavigateToQueue("NEW") }
            }
            item {
                StatCard("Recovery", stats.recoveryCount.toString(), Icons.Default.Warning) { onNavigateToQueue("RECOVERY") }
            }
            item {
                StatCard("Overdue", stats.overdueCount.toString(), Icons.Default.History) { onNavigateToQueue("OVERDUE") }
            }
            item {
                StatCard("Streak", "${stats.currentStreak} Days", Icons.Default.Whatshot, onNavigateToStatistics)
            }
            item {
                StatCard("Health Score", "${stats.healthScore}%", Icons.Default.Favorite, onNavigateToStatistics)
            }
        }

        Button(
            onClick = onNavigateToReview,
            modifier = Modifier.fillMaxWidth(),
            enabled = stats.todayReviewCount > 0
        ) {
            Text(if (stats.todayReviewCount > 0) "Start Today's Session" else "All Done for Today!")
        }
        OutlinedButton(onClick = onNavigateToManualLog, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PostAdd, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Log memorization or extra review")
        }
    }
}

@Composable
fun TodayProgressCard(current: Int, total: Int) {
    val progress = if (total > 0) current.toFloat() / total else 0f
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Today's Progress", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("$current / $total pages completed", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
