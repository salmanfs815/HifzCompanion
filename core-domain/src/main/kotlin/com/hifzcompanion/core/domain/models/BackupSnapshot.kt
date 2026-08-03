package com.hifzcompanion.core.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class BackupSnapshot(
    val version: Int = 1,
    val timestamp: Long,
    val quranPages: List<String>, // JSON serialized entities to avoid circular dependency
    val userPages: List<String>,
    val reviewEvents: List<String>,
    val schedulerStates: List<String>,
    val userSettingsEntities: List<String>,
    val userSettingsDataStore: UserSettings
)
