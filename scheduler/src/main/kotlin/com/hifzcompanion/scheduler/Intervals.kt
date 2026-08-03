package com.hifzcompanion.scheduler

object Intervals {
    val NEW_MEMORIZATION = listOf(0, 1, 2, 3, 4, 5, 6, 7, 10, 14, 21, 30)
    val RECOVERY_WEAK = listOf(1, 3, 7)
    val RECOVERY_FAILED = listOf(0, 1, 3, 7, 14, 30)
    const val LONG_TERM_ROTATION_DEFAULT = 14
}
