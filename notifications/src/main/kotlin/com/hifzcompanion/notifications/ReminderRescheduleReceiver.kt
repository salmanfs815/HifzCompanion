package com.hifzcompanion.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReminderRescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var settingsRepository: UserSettingsRepository
    @Inject lateinit var notificationScheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (!ReminderRescheduleActions.contains(intent.action)) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching {
                    val settings = settingsRepository.getUserSettings().first()
                    notificationScheduler.reschedule(settings)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
