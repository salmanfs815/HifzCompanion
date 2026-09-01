package ca.salmanfs.hifzcompanion.ui.memorization

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.core.domain.usecase.UpdateMemorizationStatusUseCase
import com.hifzcompanion.core.ui.MemorizationSelector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemorizationStatusState(
    val selectedPages: List<Int> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class MemorizationStatusViewModel @Inject constructor(
    repository: HifzRepository,
    private val updateStatus: UpdateMemorizationStatusUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(MemorizationStatusState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val selected = repository.getAllProgress().first()
                .filter { it.active && it.dateMemorized != null }
                .map { it.unitId }
                .sorted()
            _state.value = MemorizationStatusState(selectedPages = selected, loading = false)
        }
    }

    fun select(pages: List<Int>) = _state.update { it.copy(selectedPages = pages, message = null) }

    fun save(onSaved: () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(saving = true, message = null) }
        runCatching { updateStatus(_state.value.selectedPages) }
            .onSuccess { onSaved() }
            .onFailure { error -> _state.update { it.copy(saving = false, message = error.message ?: "Could not update memorization") } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorizationStatusScreen(
    viewModel: MemorizationStatusViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Current memorization") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).widthIn(max = 720.dp).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Correct the pages you have memorized. Removed pages and their review history are deleted.")
                MemorizationSelector(state.selectedPages, viewModel::select, Modifier.weight(1f).fillMaxWidth())
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = { viewModel.save(onBack) },
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.saving) "Saving…" else "Save memorization status") }
            }
        }
    }
}
