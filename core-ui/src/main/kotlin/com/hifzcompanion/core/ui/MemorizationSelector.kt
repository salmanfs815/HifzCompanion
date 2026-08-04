package com.hifzcompanion.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hifzcompanion.core.domain.quran.QuranMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorizationSelector(
    selectedPages: List<Int>,
    onSelectionChange: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${selectedPages.size} pages selected", style = MaterialTheme.typography.bodyMedium)
        PrimaryTabRow(selectedTabIndex = tab) {
            listOf("Pages", "Surahs", "Juz").forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }
        when (tab) {
            0 -> PageSelection(selectedPages, onSelectionChange, Modifier.weight(1f))
            1 -> SurahSelection(selectedPages, onSelectionChange, Modifier.weight(1f))
            else -> JuzSelection(selectedPages, onSelectionChange, Modifier.weight(1f))
        }
    }
}

@Composable
private fun PageSelection(selected: List<Int>, onChange: (List<Int>) -> Unit, modifier: Modifier) {
    var first by rememberSaveable { mutableStateOf("") }
    var last by rememberSaveable { mutableStateOf("") }
    var showIndividual by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PageNumberField(first, { first = it }, "First page", Modifier.weight(1f))
            PageNumberField(last, { last = it }, "Last page", Modifier.weight(1f))
        }
        Button(
            onClick = {
                val start = first.toIntOrNull()
                val end = last.toIntOrNull()
                if (start != null && end != null) onChange((minOf(start, end).coerceAtLeast(1)..maxOf(start, end).coerceAtMost(604)).toList())
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Use page range") }
        TextButton(onClick = { showIndividual = !showIndividual }) {
            Text(if (showIndividual) "Hide individual pages" else "Edit individual pages")
        }
        if (showIndividual) {
            LazyVerticalGrid(GridCells.Adaptive(64.dp), Modifier.weight(1f)) {
                gridItems((1..604).toList()) { page ->
                    FilterChip(
                        selected = page in selected,
                        onClick = { onChange(if (page in selected) selected - page else (selected + page).sorted()) },
                        label = { Text(page.toString()) }
                    )
                }
            }
        } else {
            Text("Enter a page range, or use the Surahs and Juz tabs.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SurahSelection(selected: List<Int>, onChange: (List<Int>) -> Unit, modifier: Modifier) {
    LazyColumn(modifier) {
        items(QuranMetadata.surahs, key = { it.number }) { surah ->
            val pages = surah.pageRange.toList()
            val checked = pages.all { it in selected }
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { enabled ->
                        onChange(if (enabled) (selected + pages).distinct().sorted() else selected - pages.toSet())
                    }
                )
                Column(Modifier.weight(1f)) {
                    Text("${surah.number}. ${surah.name}")
                    Text("Pages ${surah.pageRange.first}–${surah.pageRange.last}", style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun JuzSelection(selected: List<Int>, onChange: (List<Int>) -> Unit, modifier: Modifier) {
    LazyColumn(modifier) {
        items((1..30).toList()) { juz ->
            val pages = QuranMetadata.pagesForJuz(juz)
            val checked = pages.all { it in selected }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { enabled ->
                        onChange(if (enabled) (selected + pages).distinct().sorted() else selected - pages.toSet())
                    }
                )
                Column(Modifier.weight(1f)) {
                    Text("Juz $juz")
                    Text("Pages ${pages.first()}–${pages.last()}", style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun PageNumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
