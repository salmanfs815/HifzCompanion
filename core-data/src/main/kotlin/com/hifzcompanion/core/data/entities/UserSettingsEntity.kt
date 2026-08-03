package com.hifzcompanion.core.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "user_settings")
@Serializable
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val theme: String,
    val notificationsEnabled: Boolean,
    val preferredReviewTime: String?
)
