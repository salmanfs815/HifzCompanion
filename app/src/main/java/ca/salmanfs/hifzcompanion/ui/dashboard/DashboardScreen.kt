package ca.salmanfs.hifzcompanion.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hifzcompanion.core.domain.usecase.DashboardStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToReview: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass == androidx.window.core.layout.WindowWidthSizeClass.EXPANDED

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Dashboard") })
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
}

@Composable
fun DashboardContent(
    stats: DashboardStats,
    isExpanded: Boolean,
    onNavigateToReview: () -> Unit,
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
                StatCard("Today's Review", stats.todayReviewCount.toString(), Icons.Default.Refresh)
            }
            item {
                StatCard("New Memorization", stats.newMemorizationCount.toString(), Icons.Default.Add)
            }
            item {
                StatCard("Recovery", stats.recoveryCount.toString(), Icons.Default.Warning)
            }
            item {
                StatCard("Overdue", stats.overdueCount.toString(), Icons.Default.History)
            }
            item {
                StatCard("Streak", "${stats.currentStreak} Days", Icons.Default.Whatshot)
            }
            item {
                StatCard("Health Score", "${stats.healthScore}%", Icons.Default.Favorite)
            }
        }

        Button(
            onClick = onNavigateToReview,
            modifier = Modifier.fillMaxWidth(),
            enabled = stats.todayReviewCount > 0
        ) {
            Text(if (stats.todayReviewCount > 0) "Start Today's Session" else "All Done for Today!")
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
fun StatCard(label: String, value: String, icon: ImageVector) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}
