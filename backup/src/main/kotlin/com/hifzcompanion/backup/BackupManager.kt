package com.hifzcompanion.backup

import com.hifzcompanion.backup.crypto.CryptoManager
import com.hifzcompanion.backup.drive.GoogleDriveManager
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
    private val cryptoManager: CryptoManager,
    private val driveManager: GoogleDriveManager
) {
    private val BACKUP_FILE_NAME = "hifz_backup.bin"

    suspend fun performBackup(accessToken: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val snapshot = backupRepository.createSnapshot()
            val jsonString = Json.encodeToString(snapshot)
            val encryptedData = cryptoManager.encrypt(jsonString.toByteArray())
            
            val inputStream = ByteArrayInputStream(encryptedData)
            val fileId = driveManager.uploadBackup(accessToken, inputStream, BACKUP_FILE_NAME)
            Result.success(fileId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun performRestore(accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val inputStream = driveManager.downloadBackup(accessToken, BACKUP_FILE_NAME)
                ?: return@withContext Result.failure(Exception("Backup not found"))
            
            val encryptedData = inputStream.readBytes()
            val decryptedData = cryptoManager.decrypt(encryptedData)
            val jsonString = String(decryptedData)
            val snapshot = Json.decodeFromString<com.hifzcompanion.core.domain.models.BackupSnapshot>(jsonString)
            
            backupRepository.restoreSnapshot(snapshot)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
