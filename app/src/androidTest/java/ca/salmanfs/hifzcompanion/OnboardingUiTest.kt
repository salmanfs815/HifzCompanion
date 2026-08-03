package ca.salmanfs.hifzcompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import ca.salmanfs.hifzcompanion.ui.onboarding.GoogleSignInStep
import ca.salmanfs.hifzcompanion.ui.theme.HifzCompanionTheme
import org.junit.Rule
import org.junit.Test

class OnboardingUiTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun signInFailureIsActionableAndSkipRemainsAvailable() {
        composeRule.setContent {
            HifzCompanionTheme {
                GoogleSignInStep({}, {}, {}, isBusy = false, message = "OAuth is not configured")
            }
        }
        composeRule.onNodeWithText("OAuth is not configured").assertIsDisplayed()
        composeRule.onNodeWithText("Skip for now").assertIsDisplayed()
    }
}
