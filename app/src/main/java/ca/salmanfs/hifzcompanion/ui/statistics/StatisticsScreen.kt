package ca.salmanfs.hifzcompanion.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import com.hifzcompanion.core.domain.usecase.HealthBreakdown
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
                item(span = { GridItemSpan(maxLineSpan) }) { HealthCard(stats.health) }
            }
        }
    }
}

@Composable
private fun HealthCard(health: HealthBreakdown) {
    val tips = healthTips(health)
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(healthMessage(health.score), style = MaterialTheme.typography.titleMedium)
            HealthPart("On schedule", health.onSchedule, HealthBreakdown.SCHEDULE_POINTS)
            HealthPart("Recall strength", health.recall, HealthBreakdown.RECALL_POINTS)
            HealthPart("Daily consistency", health.consistency, HealthBreakdown.CONSISTENCY_POINTS)
            if (tips.isNotEmpty()) {
                Text("How to improve", style = MaterialTheme.typography.labelLarge)
                tips.forEach { tip -> Text("• $tip", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
private fun HealthPart(label: String, fraction: Double, points: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${(fraction * points).toInt()} / $points", style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(progress = { fraction.toFloat() }, modifier = Modifier.fillMaxWidth())
    }
}

private fun healthMessage(score: Int): String = when {
    score >= 90 -> "MashaAllah, your revision is in excellent shape. Keep up the steady routine."
    score >= 75 -> "Good, steady work. A little attention to the points below will take you to excellent."
    score >= 60 -> "Your memorization needs some attention. Small, steady steps will bring it back."
    else -> "Every hafiz goes through difficult stretches. Start small today: consistency rebuilds strength."
}

/** Tips for the parts losing at least two points, the costliest first. */
private fun healthTips(health: HealthBreakdown): List<String> {
    fun pages(count: Int) = if (count == 1) "1 page" else "$count pages"
    return listOf(
        (1 - health.onSchedule) * HealthBreakdown.SCHEDULE_POINTS to
            "${pages(health.overduePages)} overdue. Clear a few extra each day, oldest first, rather than all at once.",
        (1 - health.recall) * HealthBreakdown.RECALL_POINTS to
            "${pages(health.weakPages + health.failedPages)} last rated Weak or Failed. Give them extra attention " +
            "before your regular review; they return to rotation after consecutive Strong ratings.",
        (1 - health.consistency) * HealthBreakdown.CONSISTENCY_POINTS to
            "You reviewed on ${health.reviewDays} of the last ${health.windowDays} days. A short session every day " +
            "counts for more than a long one now and then. Reminders in Settings can help."
    ).filter { it.first >= 2 }.sortedByDescending { it.first }.map { it.second }
}
