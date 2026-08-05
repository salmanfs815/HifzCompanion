package ca.salmanfs.hifzcompanion.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hifzcompanion.core.domain.usecase.GetDashboardStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import com.hifzcompanion.analytics.HifzAnalytics

@HiltViewModel
class StatisticsViewModel @Inject constructor(getStats: GetDashboardStatsUseCase) : ViewModel() {
    val stats = getStats().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel, onBack: () -> Unit) {
    val stats = viewModel.stats.collectAsStateWithLifecycle().value
    val locale = LocalConfiguration.current.locales[0]
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Statistics") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }
        )
    }) { padding ->
        if (stats == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            val values = listOf(
                "Pages memorized" to stats.totalMemorizedPages.toString(),
                "Pages due today" to stats.pagesDueToday.toString(),
                "Average quality" to String.format(locale, "%.1f / 4", stats.averageQuality),
                "Current streak" to "${stats.currentStreak} days",
                "Longest streak" to "${stats.longestStreak} days",
                "Missed review days" to stats.missedReviewDays.toString(),
                "Backlog" to stats.backlogCount.toString(),
                "Average reviews/day" to String.format(locale, "%.1f", stats.averageReviewsPerDay),
                "Health score" to "${stats.healthScore}% • ${HifzAnalytics.healthLabel(stats.healthScore)}"
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(values) { (label, value) ->
                    ElevatedCard { Column(Modifier.padding(16.dp)) {
                        Text(label, style = MaterialTheme.typography.labelLarge)
                        Text(value, style = MaterialTheme.typography.headlineSmall)
                    } }
                }
            }
        }
    }
}
