package com.hifzcompanion.core.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "user_pages")
@Serializable
data class UserPageEntity(
    @PrimaryKey val pageId: Int,
    val strength: Int, // 0-100
    val queueType: String, // NEW, LONG_TERM, etc.
    val currentStep: Int,
    val nextReviewDate: Long, // Epoch milli
    val lastReviewDate: Long?,
    val lastResult: String?
)
