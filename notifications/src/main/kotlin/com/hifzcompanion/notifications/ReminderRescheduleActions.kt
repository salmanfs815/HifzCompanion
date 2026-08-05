package com.hifzcompanion.notifications

internal object ReminderRescheduleActions {
    const val BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
    const val MY_PACKAGE_REPLACED = "android.intent.action.MY_PACKAGE_REPLACED"

    fun contains(action: String?): Boolean = action == BOOT_COMPLETED || action == MY_PACKAGE_REPLACED
}
