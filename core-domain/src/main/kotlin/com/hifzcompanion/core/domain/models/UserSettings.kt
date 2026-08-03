package com.hifzcompanion.core.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class UserSettings(
    val onboardingCompleted: Boolean = false,
    val mushafType: MushafType = MushafType.MADINAH,
    val memorizationOrder: MemorizationOrder = MemorizationOrder.BACK_TO_FRONT,
    val dailyTargetPages: Int = 1,
    val isAutoTarget: Boolean = true
)

@Serializable
enum class MushafType {
    MADINAH,
    URDU,
    TAJWEED
}

@Serializable
enum class MemorizationOrder {
    FRONT_TO_BACK,
    BACK_TO_FRONT
}
