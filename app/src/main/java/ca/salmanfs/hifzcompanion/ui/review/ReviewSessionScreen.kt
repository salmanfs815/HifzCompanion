package ca.salmanfs.hifzcompanion.ui.review

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hifzcompanion.scheduler.models.ReviewResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewSessionScreen(
    viewModel: ReviewSessionViewModel,
    onFinished: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Review Session") },
                navigationIcon = {
                    IconButton(onClick = onFinished) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is ReviewUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ReviewUiState.Session -> {
                    if (state.isFinished) {
                        SessionFinishedContent(onFinished)
                    } else {
                        state.currentPageProgress?.let { progress ->
                            ReviewContent(
                                pageId = progress.unitId,
                                current = state.currentIndex + 1,
                                total = state.tasks.size,
                                onRate = viewModel::submitRating
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewContent(
    pageId: Int,
    current: Int,
    total: Int,
    onRate: (ReviewResult) -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Page",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = pageId.toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 120.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LinearProgressIndicator(
            progress = { current.toFloat() / total },
            modifier = Modifier.width(200.dp)
        )
        Text(
            text = "$current / $total",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "How was your recitation?",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RatingButton(
                label = "Failed",
                icon = Icons.Rounded.Error,
                color = Color(0xFFE57373),
                modifier = Modifier.weight(1f),
                onClick = { onRate(ReviewResult.FAILED) }
            )
            RatingButton(
                label = "Weak",
                icon = Icons.Rounded.Warning,
                color = Color(0xFFFFB74D),
                modifier = Modifier.weight(1f),
                onClick = { onRate(ReviewResult.WEAK) }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RatingButton(
                label = "Good",
                icon = Icons.Rounded.Check,
                color = Color(0xFFFFF176),
                textColor = Color.Black,
                modifier = Modifier.weight(1f),
                onClick = { onRate(ReviewResult.GOOD) }
            )
            RatingButton(
                label = "Strong",
                icon = Icons.Rounded.CheckCircle,
                color = Color(0xFF81C784),
                modifier = Modifier.weight(1f),
                onClick = { onRate(ReviewResult.STRONG) }
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun RatingButton(
    label: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = textColor),
        shape = MaterialTheme.shapes.large
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SessionFinishedContent(onFinished: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Celebration,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Session Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "You've reviewed all your pages for today.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onFinished) {
            Text("Back to Dashboard")
        }
    }
}
