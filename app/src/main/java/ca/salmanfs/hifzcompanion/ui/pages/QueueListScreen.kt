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
import ca.salmanfs.hifzcompanion.ui.components.SortOrderMenu
import com.hifzcompanion.core.domain.models.SessionSortOrder
import com.hifzcompanion.core.domain.repository.HifzRepository
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import com.hifzcompanion.core.domain.usecase.GetDailyReviewTasksUseCase
import com.hifzcompanion.core.domain.usecase.inSessionOrder
import com.hifzcompanion.scheduler.models.Progress
import com.hifzcompanion.scheduler.models.QueueType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class QueueListViewModel @Inject constructor(
    repository: HifzRepository,
    getDailyReviewTasksUseCase: GetDailyReviewTasksUseCase,
    private val settingsRepository: UserSettingsRepository
) : ViewModel() {
    val pages = repository.getAllProgress().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sortOrder = settingsRepository.getUserSettings()
        .map { it.sessionSortOrder }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionSortOrder.PRIORITY)

    /** Exactly the pages in today's review session, in the chosen order. */
    val todayTasks = combine(getDailyReviewTasksUseCase(), sortOrder) { tasks, order -> tasks.inSessionOrder(order) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSortOrder(order: SessionSortOrder) {
        viewModelScope.launch {
            val settings = settingsRepository.getUserSettings().first()
            settingsRepository.updateUserSettings(settings.copy(sessionSortOrder = order))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueListScreen(viewModel: QueueListViewModel, filter: String, onBack: () -> Unit, onPage: (Int) -> Unit) {
    val isToday = filter == "DUE"
    val all = viewModel.pages.collectAsStateWithLifecycle().value
    val todayTasks = viewModel.todayTasks.collectAsStateWithLifecycle().value
    val sortOrder = viewModel.sortOrder.collectAsStateWithLifecycle().value
    val today = LocalDate.now()
    val pages = if (isToday) todayTasks else all.filter {
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
            title = { Text(if (isToday) "Today's Review" else filter.lowercase().replaceFirstChar(Char::uppercase)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            actions = { if (isToday) SortOrderMenu(selected = sortOrder, onSelect = viewModel::setSortOrder) }
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
