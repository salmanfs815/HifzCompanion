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
    @Serializable
    data object Settings : Route
    @Serializable
    data object Statistics : Route
    @Serializable
    data class PageDetail(val pageId: Int) : Route
    @Serializable
    data class QueueList(val filter: String) : Route
}
