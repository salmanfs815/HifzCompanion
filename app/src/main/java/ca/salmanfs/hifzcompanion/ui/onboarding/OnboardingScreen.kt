package ca.salmanfs.hifzcompanion.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ca.salmanfs.hifzcompanion.ui.components.HifzLogo
import com.hifzcompanion.core.domain.models.MemorizationOrder
import com.hifzcompanion.core.domain.models.MushafType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isOnboardingComplete) {
        if (uiState.isOnboardingComplete) {
            onComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to Hifz Companion") },
                navigationIcon = {
                    if (uiState.currentStep > 1) {
                        IconButton(onClick = { viewModel.previousStep() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StepIndicator(currentStep = uiState.currentStep, totalSteps = 5)

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    when (uiState.currentStep) {
                        1 -> GoogleSignInStep(
                            onSignIn = { viewModel.signIn(context) },
                            onRestore = { viewModel.restoreFromCloud(context) },
                            onSkip = { viewModel.nextStep() }
                        )
                        2 -> ChooseMushafStep(
                            selected = uiState.mushafType,
                            onSelect = { viewModel.setMushafType(it) }
                        )
                        3 -> ChooseOrderStep(
                            selected = uiState.memorizationOrder,
                            onSelect = { viewModel.setMemorizationOrder(it) }
                        )
                        4 -> ProgressInputStep(
                            selectedPages = uiState.memorizedPages,
                            onTogglePage = { viewModel.togglePage(it) }
                        )
                        5 -> DailyTargetStep(
                            target = uiState.dailyTargetPages,
                            isAuto = uiState.isAutoTarget,
                            onTargetChange = { viewModel.setDailyTarget(it) },
                            onAutoToggle = { viewModel.setAutoTarget(it) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (uiState.currentStep < 5) {
                        Button(onClick = { viewModel.nextStep() }) {
                            Text("Next")
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    } else {
                        Button(onClick = { viewModel.completeOnboarding() }) {
                            Text("Finish")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepIndicator(currentStep: Int, totalSteps: Int) {
    LinearProgressIndicator(
        progress = { currentStep.toFloat() / totalSteps },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun GoogleSignInStep(
    onSignIn: () -> Unit,
    onRestore: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HifzLogo(modifier = Modifier.size(120.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("Connect your account", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Sync your progress across devices and never lose your data.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign in with Google")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Restore from Cloud")
        }
        TextButton(onClick = onSkip) {
            Text("Skip for now")
        }
    }
}

@Composable
fun ChooseMushafStep(selected: MushafType, onSelect: (MushafType) -> Unit) {
    Column {
        Text("Choose Mushaf Type", style = MaterialTheme.typography.headlineSmall)
        MushafType.entries.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected == type, onClick = { onSelect(type) })
                Text(type.name)
            }
        }
    }
}

@Composable
fun ChooseOrderStep(selected: MemorizationOrder, onSelect: (MemorizationOrder) -> Unit) {
    Column {
        Text("Memorization Order", style = MaterialTheme.typography.headlineSmall)
        MemorizationOrder.entries.forEach { order ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected == order, onClick = { onSelect(order) })
                Text(order.name.replace("_", " "))
            }
        }
    }
}

@Composable
fun ProgressInputStep(selectedPages: List<Int>, onTogglePage: (Int) -> Unit) {
    Column {
        Text("Which pages have you memorized?", style = MaterialTheme.typography.headlineSmall)
        LazyVerticalGrid(columns = GridCells.Adaptive(64.dp)) {
            items((1..604).toList()) { page ->
                FilterChip(
                    selected = selectedPages.contains(page),
                    onClick = { onTogglePage(page) },
                    label = { Text(page.toString()) }
                )
            }
        }
    }
}

@Composable
fun DailyTargetStep(
    target: Int,
    isAuto: Boolean,
    onTargetChange: (Int) -> Unit,
    onAutoToggle: (Boolean) -> Unit
) {
    Column {
        Text("Set Daily Target", style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isAuto, onCheckedChange = onAutoToggle)
            Text("Auto-calculate (Recommended)")
        }
        if (!isAuto) {
            Slider(
                value = target.toFloat(),
                onValueChange = { onTargetChange(it.toInt()) },
                valueRange = 1f..10f,
                steps = 9
            )
            Text("Target: $target pages per day")
        }
    }
}
