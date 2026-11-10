package ca.salmanfs.hifzcompanion.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.hifzcompanion.core.domain.models.SessionSortOrder

/** Top-bar action that lets the user order today's pages by priority or page number. */
@Composable
fun SortOrderMenu(selected: SessionSortOrder, onSelect: (SessionSortOrder) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort pages")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        SessionSortOrder.entries.forEach { order ->
            DropdownMenuItem(
                text = { Text(order.label) },
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.alpha(if (order == selected) 1f else 0f)
                    )
                },
                onClick = {
                    expanded = false
                    onSelect(order)
                }
            )
        }
    }
}

val SessionSortOrder.label: String
    get() = when (this) {
        SessionSortOrder.PRIORITY -> "Priority"
        SessionSortOrder.PAGE_NUMBER -> "Page number"
    }
