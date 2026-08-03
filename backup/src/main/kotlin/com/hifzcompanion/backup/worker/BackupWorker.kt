package com.hifzcompanion.backup.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.hifzcompanion.backup.BackupManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupManager: BackupManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // In a real app, we would retrieve the stored access token or refresh it.
        // For this implementation, we assume the token is available in DataStore or similar.
        // If not available, we fail.
        
        val accessToken = inputData.getString("access_token") ?: return Result.failure()
        
        val result = backupManager.performBackup(accessToken)
        
        return if (result.isSuccess) {
            Result.success()
        } else {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "daily_backup_work"

        fun schedule(context: Context, accessToken: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val data = workDataOf("access_token" to accessToken)

            val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
