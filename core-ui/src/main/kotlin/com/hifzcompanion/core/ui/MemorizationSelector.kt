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
import com.hifzcompanion.core.domain.quran.SurahInfo
import com.hifzcompanion.core.domain.quran.SurahPageRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorizationSelector(
    selectedPages: List<Int>,
    onSelectionChange: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    surahRule: SurahPageRule = SurahPageRule.ALL_TOUCHED_PAGES
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    // Surahs ticked that don't yet cover most of any page (e.g. Al-Ikhlas alone on page 604).
    var pendingSurahs by rememberSaveable { mutableStateOf(listOf<Int>()) }
    // Surahs left unticked although their pages are selected because a neighbour on a
    // shared page is memorized (e.g. An-Nas ticked, Al-Falaq not). Only valid for the
    // selection the Surahs tab last produced; edits from other tabs reset it.
    var excludedSurahs by rememberSaveable { mutableStateOf(listOf<Int>()) }
    var excludedFor by rememberSaveable { mutableStateOf(listOf<Int>()) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${selectedPages.size} pages selected", style = MaterialTheme.typography.bodyMedium)
        PrimaryTabRow(selectedTabIndex = tab) {
            listOf("Pages", "Surahs", "Juz").forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }
        when (tab) {
            0 -> PageSelection(selectedPages, onSelectionChange, Modifier.weight(1f))
            1 -> SurahSelection(
                selected = selectedPages,
                onChange = onSelectionChange,
                rule = surahRule,
                pending = pendingSurahs.toSet(),
                onPendingChange = { pendingSurahs = it.sorted() },
                excluded = if (excludedFor == selectedPages) excludedSurahs.toSet() else emptySet(),
                onExcludedChange = { surahs, pages ->
                    excludedSurahs = surahs.sorted()
                    excludedFor = pages
                },
                modifier = Modifier.weight(1f)
            )
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
private fun SurahSelection(
    selected: List<Int>,
    onChange: (List<Int>) -> Unit,
    rule: SurahPageRule,
    pending: Set<Int>,
    onPendingChange: (Set<Int>) -> Unit,
    excluded: Set<Int>,
    onExcludedChange: (Set<Int>, List<Int>) -> Unit,
    modifier: Modifier
) {
    val ownPages = remember(rule) {
        QuranMetadata.surahs.associate { it.number to QuranMetadata.pagesForSurahs(listOf(it.number), rule) }
    }
    val selectedSet = selected.toSet()
    // A surah that owns pages is ticked when all of them are selected. One that owns none
    // (it shares its page with neighbours) is ticked when its page is selected.
    fun coveredBy(pages: Set<Int>, surah: SurahInfo): Boolean =
        ownPages.getValue(surah.number).ifEmpty { surah.pageRange.toList() }.all { it in pages }
    fun coveredSurahs(pages: Set<Int>): Set<Int> =
        QuranMetadata.surahs.filter { coveredBy(pages, it) }.map { it.number }.toSet()
    val checked = (coveredSurahs(selectedSet) - excluded) + pending

    fun toggle(surah: SurahInfo, enabled: Boolean) {
        val next = if (enabled) checked + surah.number else checked - surah.number
        val before = QuranMetadata.pagesForSurahs(checked, rule).toSet()
        val after = QuranMetadata.pagesForSurahs(next, rule).toSet()
        // Only touch pages whose status this toggle changed, so pages picked elsewhere stay.
        val pages = (selectedSet - (before - after)) + after
        val sortedPages = pages.sorted()
        val covered = coveredSurahs(pages)
        onChange(sortedPages)
        onPendingChange(next - covered)
        // A neighbour on a shared page stays selected, so remember which surahs the user
        // left unticked instead of showing every surah on that page as ticked.
        onExcludedChange(covered - next, sortedPages)
    }

    LazyColumn(modifier) {
        items(QuranMetadata.surahs, key = { it.number }) { surah ->
            val own = ownPages.getValue(surah.number)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = surah.number in checked,
                    onCheckedChange = { toggle(surah, it) }
                )
                Column(Modifier.weight(1f)) {
                    Text("${surah.number}. ${surah.name}")
                    Text(surahPagesLabel(surah, own), style = MaterialTheme.typography.bodySmall)
                    if (surah.number in pending) {
                        Text(
                            "Not counted yet: tick the surahs sharing its page to log it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

private fun surahPagesLabel(surah: SurahInfo, ownPages: List<Int>): String {
    if (ownPages.isEmpty()) {
        val page = surah.pageRange.first
        val neighbours = QuranMetadata.referenceForPage(page).surahs.filter { it.number != surah.number }
        return "Shares page $page with ${neighbours.joinToString { it.name }}"
    }
    return if (ownPages.size == 1) "Page ${ownPages.first()}" else "Pages ${ownPages.first()}–${ownPages.last()}"
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
