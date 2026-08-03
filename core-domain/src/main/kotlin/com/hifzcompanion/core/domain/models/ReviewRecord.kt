package com.hifzcompanion.core.domain.models

import com.hifzcompanion.scheduler.models.ReviewResult
import kotlinx.serialization.Serializable

@Serializable
data class ReviewRecord(
    val id: Int,
    val pageId: Int,
    val timestamp: Long,
    val quality: ReviewResult,
    val reviewType: ReviewType,
    val durationSeconds: Int = 0,
    val notes: String? = null
)

@Serializable
enum class ReviewType { SCHEDULED, MANUAL, RECOVERY, NEW }
