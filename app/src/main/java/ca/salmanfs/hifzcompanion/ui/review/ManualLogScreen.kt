package ca.salmanfs.hifzcompanion.ui.review

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hifzcompanion.core.domain.quran.SurahPageRule
import com.hifzcompanion.core.domain.usecase.LogManualReviewUseCase
import com.hifzcompanion.core.ui.MemorizationSelector
import com.hifzcompanion.scheduler.models.ReviewResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManualLogState(
    val pages: List<Int> = emptyList(),
    val newlyMemorized: Boolean = false,
    val notes: String = "",
    val saving: Boolean = false,
    val message: String? = null,
    val complete: Boolean = false
)

@HiltViewModel
class ManualLogViewModel @Inject constructor(
    private val logManualReview: LogManualReviewUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(ManualLogState())
    val state = _state.asStateFlow()

    fun setPages(pages: List<Int>) = _state.update { it.copy(pages = pages, message = null) }
    fun setMode(newlyMemorized: Boolean) = _state.update { it.copy(newlyMemorized = newlyMemorized) }
    fun setNotes(notes: String) = _state.update { it.copy(notes = notes) }

    fun submit(result: ReviewResult) = viewModelScope.launch {
        val current = _state.value
        if (current.pages.isEmpty()) {
            _state.update { it.copy(message = "Select at least one page, Surah, or Juz.") }
            return@launch
        }
        _state.update { it.copy(saving = true, message = null) }
        runCatching { logManualReview(current.pages, result, current.newlyMemorized, current.notes.ifBlank { null }) }
            .onSuccess { _state.update { it.copy(saving = false, complete = true) } }
            .onFailure { error -> _state.update { it.copy(saving = false, message = error.message ?: "Could not save this review") } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLogScreen(viewModel: ManualLogViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log memorization or review") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (state.complete) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.CheckCircle, null, Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Progress updated", style = MaterialTheme.typography.headlineSmall)
                Text("These pages were rescheduled and removed from today's queue.")
                Spacer(Modifier.height(24.dp))
                Button(onClick = onBack) { Text("Back to dashboard") }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).widthIn(max = 720.dp).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !state.newlyMemorized,
                        onClick = { viewModel.setMode(false) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Reviewed") }
                    SegmentedButton(
                        selected = state.newlyMemorized,
                        onClick = { viewModel.setMode(true) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Memorized") }
                }
                Text(
                    if (state.newlyMemorized) "Select what you newly memorized." else "Select what you reviewed outside today's schedule.",
                    style = MaterialTheme.typography.bodyMedium
                )
                MemorizationSelector(
                    selectedPages = state.pages,
                    onSelectionChange = viewModel::setPages,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    // Log only pages mostly covered by the surahs recited, not the neighbours' edge pages.
                    surahRule = SurahPageRule.MAJORITY_OF_PAGE
                )
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::setNotes,
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                Text("How strong was the recitation?", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RatingButton("Failed", Icons.Rounded.Error, Color(0xFFE57373), Modifier.weight(1f)) { viewModel.submit(ReviewResult.FAILED) }
                    RatingButton("Weak", Icons.Rounded.Warning, Color(0xFFFFB74D), Modifier.weight(1f)) { viewModel.submit(ReviewResult.WEAK) }
                    RatingButton("Good", Icons.Rounded.Check, Color(0xFFFFF176), Modifier.weight(1f), Color.Black) { viewModel.submit(ReviewResult.GOOD) }
                    RatingButton("Strong", Icons.Rounded.CheckCircle, Color(0xFF81C784), Modifier.weight(1f)) { viewModel.submit(ReviewResult.STRONG) }
                }
                if (state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
