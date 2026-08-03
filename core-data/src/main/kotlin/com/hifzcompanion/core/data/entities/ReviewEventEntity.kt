package com.hifzcompanion.core.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "review_events")
@Serializable
data class ReviewEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pageId: Int,
    val timestamp: Long,
    val result: String,
    val difficulty: Int,
    val reviewType: String = "SCHEDULED",
    val durationSeconds: Int = 0,
    val notes: String? = null
)
