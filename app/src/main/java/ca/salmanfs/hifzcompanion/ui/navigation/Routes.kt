package ca.salmanfs.hifzcompanion.ui.navigation

import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Onboarding : Route
    @Serializable
    data object Dashboard : Route
    @Serializable
    data object ReviewSession : Route
}
