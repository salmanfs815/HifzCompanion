package com.hifzcompanion.backup.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.hifzcompanion.backup.BackupManager
import com.hifzcompanion.backup.auth.BackupSessionStore
import com.hifzcompanion.backup.auth.GoogleAuthManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupManager: BackupManager,
    private val sessionStore: BackupSessionStore,
    private val authManager: GoogleAuthManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val accessToken = runCatching {
            authManager.requestDriveAuthorization(applicationContext)
        }.getOrElse {
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
        sessionStore.saveAccessToken(accessToken)
        val result = backupManager.performBackup(accessToken)
        
        return if (result.isSuccess) {
            Result.success()
        } else {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "daily_backup_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
