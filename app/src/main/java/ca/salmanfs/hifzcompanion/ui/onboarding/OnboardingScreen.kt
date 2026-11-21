package ca.salmanfs.hifzcompanion.ui.onboarding

import androidx.compose.foundation.layout.*
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
import ca.salmanfs.hifzcompanion.ui.guide.AppGuideScreen
import androidx.compose.runtime.saveable.rememberSaveable
import com.hifzcompanion.core.domain.models.MemorizationOrder
import com.hifzcompanion.core.domain.models.MushafType
import com.hifzcompanion.core.ui.MemorizationSelector
import android.app.Activity
import android.app.PendingIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val authorizationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.completeDriveAuthorization(result.data.takeIf { result.resultCode == Activity.RESULT_OK })
    }
    val launchConsent: (PendingIntent) -> Unit = { pendingIntent ->
        authorizationLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
    }

    LaunchedEffect(uiState.isOnboardingComplete) {
        if (uiState.isOnboardingComplete) {
            onComplete()
        }
    }

    var showGuide by rememberSaveable { mutableStateOf(true) }
    if (showGuide) {
        AppGuideScreen(onFinish = { showGuide = false }, finishLabel = "Set up")
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to Hifz Companion") },
                navigationIcon = {
                    if (uiState.currentStep == 1) {
                        IconButton(onClick = { showGuide = true }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to introduction")
                        }
                    } else {
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
                StepIndicator(currentStep = uiState.currentStep, totalSteps = 4)

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (uiState.currentStep) {
                    1 -> GoogleSignInStep(
                            onSignIn = { viewModel.connectGoogleDrive(context, restore = false, launchConsent = launchConsent) },
                            onRestore = { viewModel.connectGoogleDrive(context, restore = true, launchConsent = launchConsent) },
                            onSkip = { viewModel.nextStep() },
                            isBusy = uiState.isBusy,
                            message = uiState.message
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
                            onSetPages = { viewModel.setMemorizedPages(it) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (uiState.currentStep < 4) {
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
    onSkip: () -> Unit,
    isBusy: Boolean = false,
    message: String? = null
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
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBusy
        ) {
            Text("Connect Google Drive")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onRestore,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isBusy
        ) {
            Text("Restore from Cloud")
        }
        if (isBusy) CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        message?.let {
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        TextButton(onClick = onSkip, enabled = !isBusy) {
            Text("Skip for now")
        }
    }
}

@Composable
fun ChooseMushafStep(selected: MushafType, onSelect: (MushafType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Choose Mushaf Type", style = MaterialTheme.typography.headlineSmall)
        MushafType.supported.forEach { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected == type, onClick = { onSelect(type) })
                Text(type.label)
            }
        }
        Text(
            "Page numbers follow this layout. Other mushaf layouts are not supported yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
fun ProgressInputStep(
    selectedPages: List<Int>,
    onSetPages: (List<Int>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("How much have you memorized?", style = MaterialTheme.typography.headlineSmall)
        Text("Choose by page, scroll through Surah names, or select complete Juz.", style = MaterialTheme.typography.bodyMedium)
        MemorizationSelector(
            selectedPages = selectedPages,
            onSelectionChange = onSetPages,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}
