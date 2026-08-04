package ca.salmanfs.hifzcompanion.ui.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hifzcompanion.core.domain.models.ReviewRecord
import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.core.domain.quran.QuranMetadata
import com.hifzcompanion.scheduler.models.Progress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

data class PageDetailState(val progress: Progress? = null, val history: List<ReviewRecord> = emptyList())

@HiltViewModel
class PageDetailViewModel @Inject constructor(private val repository: HifzRepository) : ViewModel() {
    fun state(pageId: Int): StateFlow<PageDetailState> = combine(
        repository.getProgress(pageId), repository.getReviewEvents(pageId)
    ) { progress, history -> PageDetailState(progress, history) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PageDetailState())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageDetailScreen(viewModel: PageDetailViewModel, pageId: Int, onBack: () -> Unit) {
    val reference = remember(pageId) { QuranMetadata.referenceForPage(pageId) }
    val stateFlow = remember(pageId) { viewModel.state(pageId) }
    val state = stateFlow.collectAsStateWithLifecycle().value
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Page $pageId") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).widthIn(max = 720.dp).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val progress = state.progress
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Juz ${reference.juz}", style = MaterialTheme.typography.titleMedium)
                        Text(reference.ayahLabel.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                        HorizontalDivider()
                        Text("Status", style = MaterialTheme.typography.titleMedium)
                        Text(progress?.stage?.name?.replace('_', ' ') ?: "Not tracked")
                        Text("Strength: ${progress?.strength ?: 0}%")
                        Text("Memorized: ${progress?.dateMemorized ?: "Not yet"}")
                        Text("Last review: ${progress?.lastReviewDate ?: "Never"}")
                        Text("Next review: ${progress?.nextReviewDate ?: "Not scheduled"}")
                        progress?.notes?.takeIf { it.isNotBlank() }?.let { Text("Notes: $it") }
                    }
                }
            }
            item { Text("Review history", style = MaterialTheme.typography.titleLarge) }
            if (state.history.isEmpty()) item { Text("No reviews recorded yet.") }
            items(state.history, key = { it.id }) { event ->
                val date = Instant.ofEpochMilli(event.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                ListItem(
                    headlineContent = { Text(event.quality.name.lowercase().replaceFirstChar(Char::uppercase)) },
                    supportingContent = { Text("$date • ${event.reviewType.name.lowercase()}") },
                    trailingContent = { if (event.durationSeconds > 0) Text("${event.durationSeconds}s") }
                )
                HorizontalDivider()
            }
        }
    }
}
