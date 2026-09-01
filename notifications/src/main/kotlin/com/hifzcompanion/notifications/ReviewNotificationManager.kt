package com.hifzcompanion.notifications

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun dismissDailyReminders() {
        val manager = NotificationManagerCompat.from(context)
        manager.cancel(ReviewNotificationWorker.MORNING_NOTIFICATION_ID)
        manager.cancel(ReviewNotificationWorker.EVENING_NOTIFICATION_ID)
    }
}
