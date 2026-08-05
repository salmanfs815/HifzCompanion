package com.hifzcompanion.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hifzcompanion.core.domain.usecase.GetDashboardStatsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.Locale

@HiltWorker
class ReviewNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val getStats: GetDashboardStatsUseCase
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val kind = runCatching { NotificationKind.valueOf(inputData.getString(KEY_KIND).orEmpty()) }.getOrElse { return Result.failure() }
        val stats = getStats().first()
        val (title, body) = when (kind) {
            NotificationKind.MORNING -> "Today's hifz review" to "${stats.todayReviewCount} pages are ready to review."
            NotificationKind.EVENING -> "Finish today's review" to "${stats.todayReviewCount} pages remain."
            NotificationKind.WEEKLY -> "Your weekly hifz summary" to "Average ${String.format(Locale.getDefault(), "%.1f", stats.averageReviewsPerDay)} reviews/day • ${stats.currentStreak}-day streak."
        }
        if (kind != NotificationKind.WEEKLY && stats.todayReviewCount == 0) return Result.success()
        createChannel()
        if (android.os.Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return Result.success()
        }
        val contentIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            ?.let { launchIntent ->
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notifySafely(kind.ordinal + 100, notification)
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(id: Int, notification: android.app.Notification) {
        runCatching { NotificationManagerCompat.from(applicationContext).notify(id, notification) }
    }

    private fun createChannel() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Hifz review reminders", NotificationManager.IMPORTANCE_DEFAULT))
    }

    companion object {
        const val KEY_KIND = "notification_kind"
        private const val CHANNEL_ID = "hifz_review_reminders"
    }
}
