package com.hifzcompanion.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderRescheduleActionsTest {
    @Test
    fun `boot and app replacement trigger reminder recovery`() {
        assertTrue(ReminderRescheduleActions.contains(ReminderRescheduleActions.BOOT_COMPLETED))
        assertTrue(ReminderRescheduleActions.contains(ReminderRescheduleActions.MY_PACKAGE_REPLACED))
    }

    @Test
    fun `unrelated and missing actions are ignored`() {
        assertFalse(ReminderRescheduleActions.contains("android.intent.action.TIMEZONE_CHANGED"))
        assertFalse(ReminderRescheduleActions.contains(null))
    }
}
