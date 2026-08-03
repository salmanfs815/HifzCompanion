package com.hifzcompanion.core.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "scheduler_state")
@Serializable
data class SchedulerStateEntity(
    @PrimaryKey val id: Int = 1,
    val lastUpdated: Long,
    val dailyCapacity: Int
)
