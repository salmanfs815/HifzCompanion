package com.hifzcompanion.analytics

object HifzAnalytics {
    fun healthLabel(score: Int): String = when (score.coerceIn(0, 100)) {
        in 90..100 -> "Excellent"
        in 75..89 -> "Good"
        in 60..74 -> "Needs attention"
        else -> "Recovery recommended"
    }

    fun qualityLabel(average: Double): String = when {
        average >= 3.5 -> "Strong"
        average >= 2.5 -> "Good"
        average >= 1.5 -> "Weak"
        average > 0.0 -> "At risk"
        else -> "No reviews yet"
    }
}
