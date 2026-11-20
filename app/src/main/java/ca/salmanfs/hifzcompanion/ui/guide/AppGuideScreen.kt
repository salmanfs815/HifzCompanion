package ca.salmanfs.hifzcompanion.ui.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ca.salmanfs.hifzcompanion.ui.components.HifzLogo
import kotlinx.coroutines.launch

private data class GuidePoint(val icon: ImageVector, val title: String, val body: String)

private data class GuidePage(
    val title: String,
    val intro: String,
    val points: List<GuidePoint> = emptyList(),
    val showLogo: Boolean = false
)

private val guidePages = listOf(
    GuidePage(
        title = "Assalamu alaikum",
        intro = "Hifz Companion helps you keep what you have memorized of the Qur'an strong. " +
            "It turns your memorized pages into a manageable daily revision plan, so the right " +
            "pages come up at the right time and none quietly slip away.",
        points = listOf(
            GuidePoint(Icons.Default.Lock, "Private and offline", "Everything stays on your device. No account is needed; Google Drive backup is optional."),
            GuidePoint(Icons.Default.Schedule, "Spaced revision", "Pages you know well come up less often, pages you struggle with come up sooner.")
        ),
        showLogo = true
    ),
    GuidePage(
        title = "How your revision is organized",
        intro = "Your dashboard groups each day's work into queues:",
        points = listOf(
            GuidePoint(Icons.Default.Refresh, "Today's Review", "Pages due from your long-term rotation through everything you have memorized."),
            GuidePoint(Icons.Default.Add, "New Memorization", "Recently memorized pages, reviewed on a short schedule until they are stable."),
            GuidePoint(Icons.Default.Warning, "Recovery", "Pages rated Weak or Failed get extra attention until you recite them strongly again."),
            GuidePoint(Icons.Default.History, "Overdue", "Pages past their due date. The app caps each day's load so a backlog stays realistic.")
        )
    ),
    GuidePage(
        title = "Rate each recitation honestly",
        intro = "After reciting a page from memory, choose how it went. The rating decides when you will see it next.",
        points = listOf(
            GuidePoint(Icons.Default.Star, "Strong", "Fluent and confident. The page waits longer before coming back."),
            GuidePoint(Icons.Default.ThumbUp, "Good", "Correct with minor hesitation. Advances normally."),
            GuidePoint(Icons.Default.Warning, "Weak", "Needed prompting or hesitated repeatedly. Moves to Recovery."),
            GuidePoint(Icons.Default.Close, "Failed", "Could not complete it reliably. Starts a dedicated recovery schedule.")
        )
    ),
    GuidePage(
        title = "Your daily routine",
        intro = "A few minutes each day is all it takes:",
        points = listOf(
            GuidePoint(Icons.Default.PlayArrow, "Start today's session", "Open the dashboard and start your review. Recite each page, then rate it."),
            GuidePoint(Icons.Default.PostAdd, "Log everything else", "Revised outside the plan, or memorized a new page? Use \"Log memorization or extra review\" so the schedule knows."),
            GuidePoint(Icons.Default.History, "Fix mistakes", "Tapped the wrong rating? Today's activity (history icon) lets you correct it."),
            GuidePoint(Icons.Default.Notifications, "Set reminders", "Morning, evening and weekly reminders can be turned on in Settings.")
        )
    ),
    GuidePage(
        title = "Getting the most out of it",
        intro = "Some tips for effective revision:",
        points = listOf(
            GuidePoint(Icons.Default.Whatshot, "Consistency beats volume", "A short session every day keeps your streak and health score up better than an occasional long one."),
            GuidePoint(Icons.Default.Visibility, "Recite before you look", "Try each page fully from memory first, then check. Rate what you actually recited."),
            GuidePoint(Icons.Default.Tune, "Adjust to your pace", "In Settings you can change daily capacity, schedules, and which pages you have memorized."),
            GuidePoint(Icons.Default.CloudUpload, "Back up your progress", "Connect Google Drive in Settings so you can restore on a new phone."),
            GuidePoint(Icons.Default.Info, "Come back anytime", "Open this guide again from the info button on the dashboard.")
        )
    )
)

/**
 * Swipeable introduction to the app. Shown before setup during onboarding and from the
 * dashboard's info button. [onFinish] runs on skip as well as on the last page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppGuideScreen(
    onFinish: () -> Unit,
    finishLabel: String = "Done",
    onBack: (() -> Unit)? = null
) {
    val pagerState = rememberPagerState { guidePages.size }
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == guidePages.lastIndex

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Hifz Companion") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!isLast) TextButton(onClick = onFinish) { Text("Skip") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) { index ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    GuidePageContent(guidePages[index], Modifier.widthIn(max = 720.dp))
                }
            }
            PageDots(count = guidePages.size, current = pagerState.currentPage)
            Row(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } }) {
                        Text("Previous")
                    }
                } else {
                    Spacer(Modifier)
                }
                Button(onClick = {
                    if (isLast) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) {
                    Text(if (isLast) finishLabel else "Next")
                }
            }
        }
    }
}

@Composable
private fun GuidePageContent(page: GuidePage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (page.showLogo) {
            HifzLogo(modifier = Modifier.size(96.dp).align(Alignment.CenterHorizontally))
        }
        Text(
            page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = if (page.showLogo) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(page.intro, style = MaterialTheme.typography.bodyLarge)
        page.points.forEach { point ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        point.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(point.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        point.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        repeat(count) { index ->
            Box(
                Modifier
                    .size(if (index == current) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}
