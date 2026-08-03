package ca.salmanfs.hifzcompanion.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.QueueType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class QueueListViewModel @Inject constructor(repository: HifzRepository) : ViewModel() {
    val pages = repository.getAllProgress().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueListScreen(viewModel: QueueListViewModel, filter: String, onBack: () -> Unit, onPage: (Int) -> Unit) {
    val all = viewModel.pages.collectAsStateWithLifecycle().value
    val today = LocalDate.now()
    val pages = all.filter {
        when (filter) {
            "NEW" -> it.queueType == QueueType.NEW
            "RECOVERY" -> it.queueType == QueueType.RECOVERY_WEAK || it.queueType == QueueType.RECOVERY_FAILED
            "OVERDUE" -> it.nextReviewDate < today
            "MEMORIZED" -> it.dateMemorized != null
            else -> it.nextReviewDate <= today
        }
    }.sortedBy { it.nextReviewDate }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(filter.lowercase().replaceFirstChar(Char::uppercase)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } }
        )
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            if (pages.isEmpty()) item { Text("No pages in this list.", Modifier.padding(24.dp)) }
            items(pages, key = Progress::unitId) { page ->
                ListItem(
                    headlineContent = { Text("Page ${page.unitId}") },
                    supportingContent = { Text("${page.stage.name.lowercase()} • due ${page.nextReviewDate}") },
                    modifier = Modifier.clickable { onPage(page.unitId) }
                )
                HorizontalDivider()
            }
        }
    }
}
