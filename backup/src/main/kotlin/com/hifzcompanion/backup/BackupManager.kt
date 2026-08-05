package com.hifzcompanion.backup

import com.hifzcompanion.backup.drive.BackupStorage
import com.hifzcompanion.core.domain.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val backupRepository: BackupRepository,
    private val driveManager: BackupStorage
) {
    private val backupFileName = "hifz_backup.json"
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun performBackup(accessToken: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val snapshot = backupRepository.createSnapshot()
            val jsonString = json.encodeToString(snapshot)
            val inputStream = ByteArrayInputStream(jsonString.toByteArray(Charsets.UTF_8))
            val fileId = driveManager.uploadBackup(accessToken, inputStream, backupFileName)
            Result.success(fileId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun performRestore(accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val inputStream = driveManager.downloadBackup(accessToken, backupFileName)
                ?: return@withContext Result.failure(Exception("Backup not found"))
            
            val jsonString = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val snapshot = json.decodeFromString<com.hifzcompanion.core.domain.models.BackupSnapshot>(jsonString)
            
            backupRepository.restoreSnapshot(snapshot)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
