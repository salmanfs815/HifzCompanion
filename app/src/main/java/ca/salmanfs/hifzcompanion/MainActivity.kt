package ca.salmanfs.hifzcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val onboardingCompleted by mainViewModel.isOnboardingCompleted.collectAsState()

            HifzCompanionTheme {
                if (onboardingCompleted != null) {
                    val initialRoute = if (onboardingCompleted == true) Route.Dashboard else Route.Onboarding
                    val backStack = remember { mutableStateListOf<Route>(initialRoute) }
                    
                    NavDisplay(
                        backStack = backStack,
                        onBack = { backStack.removeLastOrNull() },
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
                                    }
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
                        }
                    }
                }
            }
        }
    }
}
