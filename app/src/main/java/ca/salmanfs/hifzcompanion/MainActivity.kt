package ca.salmanfs.hifzcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import ca.salmanfs.hifzcompanion.ui.dashboard.DashboardScreen
import ca.salmanfs.hifzcompanion.ui.dashboard.DashboardViewModel
import ca.salmanfs.hifzcompanion.ui.navigation.Route
import ca.salmanfs.hifzcompanion.ui.onboarding.OnboardingScreen
import ca.salmanfs.hifzcompanion.ui.onboarding.OnboardingViewModel
import ca.salmanfs.hifzcompanion.ui.review.ReviewSessionScreen
import ca.salmanfs.hifzcompanion.ui.review.ReviewSessionViewModel
import ca.salmanfs.hifzcompanion.ui.theme.HifzCompanionTheme
import ca.salmanfs.hifzcompanion.ui.statistics.StatisticsScreen
import ca.salmanfs.hifzcompanion.ui.statistics.StatisticsViewModel
import ca.salmanfs.hifzcompanion.ui.pages.PageDetailScreen
import ca.salmanfs.hifzcompanion.ui.pages.PageDetailViewModel
import ca.salmanfs.hifzcompanion.ui.pages.QueueListScreen
import ca.salmanfs.hifzcompanion.ui.pages.QueueListViewModel
import com.hifzcompanion.core.domain.models.AppTheme
import com.hifzcompanion.settings.ui.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val onboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsState()
            val userSettings by mainViewModel.userSettings.collectAsState()
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
            LaunchedEffect(userSettings?.notificationsEnabled, userSettings?.onboardingCompleted) {
                if (Build.VERSION.SDK_INT >= 33 && userSettings?.notificationsEnabled == true && userSettings?.onboardingCompleted == true) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            val darkTheme = when (userSettings?.theme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                else -> isSystemInDarkTheme()
            }

            HifzCompanionTheme(darkTheme = darkTheme) {
                if (onboardingCompleted != null) {
                    val initialRoute = if (onboardingCompleted == true) Route.Dashboard else Route.Onboarding
                    val backStack = remember { mutableStateListOf<Route>(initialRoute) }
                    
                    NavDisplay(
                        backStack = backStack,
                        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                        entryDecorators = listOf(
                            rememberViewModelStoreNavEntryDecorator()
                        )
                    ) { key ->
                        when (key) {
                            Route.Onboarding -> NavEntry(key) {
                                val viewModel: OnboardingViewModel = hiltViewModel()
                                OnboardingScreen(
                                    viewModel = viewModel,
                                    onComplete = { 
                                        backStack.clear()
                                        backStack.add(Route.Dashboard)
                                    }
                                )
                            }
                            Route.Dashboard -> NavEntry(key) {
                                val viewModel: DashboardViewModel = hiltViewModel()
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToReview = { 
                                        backStack.add(Route.ReviewSession)
                                    },
                                    onNavigateToSettings = { backStack.add(Route.Settings) },
                                    onNavigateToStatistics = { backStack.add(Route.Statistics) },
                                    onNavigateToQueue = { backStack.add(Route.QueueList(it)) }
                                )
                            }
                            Route.ReviewSession -> NavEntry(key) {
                                val viewModel: ReviewSessionViewModel = hiltViewModel()
                                ReviewSessionScreen(
                                    viewModel = viewModel,
                                    onFinished = {
                                        backStack.removeLastOrNull()
                                    }
                                )
                            }
                            Route.Settings -> NavEntry(key) {
                                SettingsScreen(
                                    onBackClick = { backStack.removeLastOrNull() },
                                    serverClientId = BuildConfig.GOOGLE_SERVER_CLIENT_ID
                                )
                            }
                            Route.Statistics -> NavEntry(key) {
                                val viewModel: StatisticsViewModel = hiltViewModel()
                                StatisticsScreen(viewModel, onBack = { backStack.removeLastOrNull() })
                            }
                            is Route.PageDetail -> NavEntry(key) {
                                val viewModel: PageDetailViewModel = hiltViewModel()
                                PageDetailScreen(viewModel, key.pageId, onBack = { backStack.removeLastOrNull() })
                            }
                            is Route.QueueList -> NavEntry(key) {
                                val viewModel: QueueListViewModel = hiltViewModel()
                                QueueListScreen(
                                    viewModel = viewModel,
                                    filter = key.filter,
                                    onBack = { backStack.removeLastOrNull() },
                                    onPage = { backStack.add(Route.PageDetail(it)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
