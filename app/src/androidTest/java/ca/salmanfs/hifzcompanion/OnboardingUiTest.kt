package ca.salmanfs.hifzcompanion

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import ca.salmanfs.hifzcompanion.ui.onboarding.GoogleSignInStep
import ca.salmanfs.hifzcompanion.ui.theme.HifzCompanionTheme
import org.junit.Rule
import org.junit.Test

class OnboardingUiTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComposeTestActivity>()

    @Test fun signInFailureIsActionableAndSkipRemainsAvailable() {
        composeRule.setContent {
            HifzCompanionTheme {
                GoogleSignInStep({}, {}, {}, isBusy = false, message = "OAuth is not configured")
            }
        }
        composeRule.onNodeWithText("OAuth is not configured").assertIsDisplayed()
        composeRule.onNodeWithText("Skip for now").assertIsDisplayed()
    }

    @Test fun restoreIsAvailableWithoutAdditionalCredentials() {
        composeRule.setContent {
            HifzCompanionTheme {
                GoogleSignInStep({}, {}, {})
            }
        }
        composeRule.onNodeWithText("Restore from Cloud").assertIsDisplayed()
    }
}
