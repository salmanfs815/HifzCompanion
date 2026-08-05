package com.hifzcompanion.backup.drive

import java.io.InputStream

interface BackupStorage {
    suspend fun uploadBackup(accessToken: String, inputStream: InputStream, fileName: String): String
    suspend fun downloadBackup(accessToken: String, fileName: String): InputStream?
}
