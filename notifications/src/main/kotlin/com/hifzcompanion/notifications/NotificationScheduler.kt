package com.hifzcompanion.notifications

import android.content.Context
import androidx.work.*
import com.hifzcompanion.core.domain.models.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.*
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun apply(settings: UserSettings) {
        schedule(settings, ExistingPeriodicWorkPolicy.UPDATE)
    }

    fun reschedule(settings: UserSettings) {
        schedule(settings, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE)
    }

    private fun schedule(settings: UserSettings, policy: ExistingPeriodicWorkPolicy) {
        if (!settings.notificationsEnabled) {
            cancelAll()
            return
        }
        scheduleDaily("morning_review_notification", NotificationKind.MORNING, settings.morningNotificationTime, policy)
        scheduleDaily("evening_review_notification", NotificationKind.EVENING, settings.eveningNotificationTime, policy)
        scheduleWeekly(settings.weeklyNotificationDay, settings.eveningNotificationTime, policy)
    }

    fun cancelAll() {
        listOf("morning_review_notification", "evening_review_notification", "weekly_review_notification")
            .forEach { WorkManager.getInstance(context).cancelUniqueWork(it) }
    }

    private fun scheduleDaily(name: String, kind: NotificationKind, time: String, policy: ExistingPeriodicWorkPolicy) {
        val delay = Duration.between(ZonedDateTime.now(), nextDaily(time)).toMillis().coerceAtLeast(0)
        val request = PeriodicWorkRequestBuilder<ReviewNotificationWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReviewNotificationWorker.KEY_KIND to kind.name))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(name, policy, request)
    }

    private fun scheduleWeekly(day: Int, time: String, policy: ExistingPeriodicWorkPolicy) {
        val now = ZonedDateTime.now()
        val targetDay = DayOfWeek.of(day.coerceIn(1, 7))
        var target = now.with(TemporalAdjusters.nextOrSame(targetDay)).with(parseTime(time)).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusWeeks(1)
        val request = PeriodicWorkRequestBuilder<ReviewNotificationWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(Duration.between(now, target).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReviewNotificationWorker.KEY_KIND to NotificationKind.WEEKLY.name))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("weekly_review_notification", policy, request)
    }

    private fun nextDaily(time: String): ZonedDateTime {
        val now = ZonedDateTime.now()
        var target = now.with(parseTime(time)).withSecond(0).withNano(0)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target
    }

    private fun parseTime(value: String) = runCatching { LocalTime.parse(value) }.getOrDefault(LocalTime.of(8, 0))
}

enum class NotificationKind { MORNING, EVENING, WEEKLY }
