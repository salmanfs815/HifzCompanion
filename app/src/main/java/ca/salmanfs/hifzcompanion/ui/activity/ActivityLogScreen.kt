package ca.salmanfs.hifzcompanion.ui.activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hifzcompanion.core.domain.models.ReviewRecord
import com.hifzcompanion.core.domain.quran.QuranMetadata
import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.core.domain.usecase.EditReviewEventUseCase
import com.hifzcompanion.scheduler.models.ReviewResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ActivityLogState(
    val events: List<ReviewRecord> = emptyList(),
    val savingEventId: Int? = null,
    val message: String? = null
)

@HiltViewModel
class ActivityLogViewModel @Inject constructor(
    repository: HifzRepository,
    private val editReview: EditReviewEventUseCase
) : ViewModel() {
    private val savingEventId = MutableStateFlow<Int?>(null)
    private val message = MutableStateFlow<String?>(null)
    val state: StateFlow<ActivityLogState> = combine(
        repository.getAllReviewEvents(), savingEventId, message
    ) { events, saving, status ->
        val today = LocalDate.now()
        ActivityLogState(
            events = events.filter { it.localDate() == today }.sortedByDescending { it.timestamp },
            savingEventId = saving,
            message = status
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActivityLogState())

    fun changeQuality(event: ReviewRecord, quality: ReviewResult) = viewModelScope.launch {
        savingEventId.value = event.id
        message.value = null
        runCatching { editReview(event, quality) }
            .onSuccess { message.value = "Page ${event.pageId} updated and rescheduled." }
            .onFailure { message.value = it.message ?: "Could not update this review." }
        savingEventId.value = null
    }

    private fun ReviewRecord.localDate(): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(viewModel: ActivityLogViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ReviewRecord?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Today's activity") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).widthIn(max = 720.dp).padding(horizontal = 16.dp)) {
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp)) }
            if (state.events.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No reviews have been recorded today.")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text(
                            "${state.events.size} review${if (state.events.size == 1) "" else "s"} today",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(state.events, key = { it.id }) { event ->
                        val reference = remember(event.pageId) { QuranMetadata.referenceForPage(event.pageId) }
                        val time = remember(event.timestamp) {
                            Instant.ofEpochMilli(event.timestamp).atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("h:mm a"))
                        }
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            ListItem(
                                headlineContent = { Text("Page ${event.pageId} • ${event.quality.displayName()}") },
                                supportingContent = {
                                    Text("Juz ${reference.juz} • ${reference.ayahLabel}\n$time • ${event.reviewType.name.lowercase().replaceFirstChar(Char::uppercase)}")
                                },
                                trailingContent = {
                                    if (state.savingEventId == event.id) {
                                        CircularProgressIndicator(Modifier.size(24.dp))
                                    } else {
                                        IconButton(onClick = { editing = event }) { Icon(Icons.Rounded.Edit, "Change revision strength") }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    editing?.let { event ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Change page ${event.pageId} strength") },
            text = {
                Column {
                    ReviewResult.entries.forEach { quality ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                viewModel.changeQuality(event, quality)
                                editing = null
                            }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = event.quality == quality, onClick = null)
                            Text(quality.displayName())
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }
}

private fun ReviewResult.displayName() = name.lowercase().replaceFirstChar(Char::uppercase)
