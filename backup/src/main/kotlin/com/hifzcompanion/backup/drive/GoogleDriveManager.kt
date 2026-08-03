package com.hifzcompanion.backup.drive

import android.content.Context
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.InputStreamContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

    suspend fun uploadBackup(accessToken: String, inputStream: InputStream, fileName: String): String = withContext(Dispatchers.IO) {
        val driveService = getDriveService(accessToken)

        val fileMetadata = File().apply {
            name = fileName
            parents = Collections.singletonList("appDataFolder")
        }

        val mediaContent = InputStreamContent("application/octet-stream", inputStream)
        
        // Check if file already exists
        val existingFiles = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$fileName'")
            .execute()
            .files

        if (existingFiles.isNullOrEmpty()) {
            val file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
            file.id
        } else {
            val fileId = existingFiles[0].id
            val file = driveService.files().update(fileId, null, mediaContent)
                .setFields("id")
                .execute()
            file.id
        }
    }

    suspend fun downloadBackup(accessToken: String, fileName: String): InputStream? = withContext(Dispatchers.IO) {
        val driveService = getDriveService(accessToken)

        val existingFiles = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$fileName'")
            .execute()
            .files

        if (existingFiles.isNullOrEmpty()) {
            null
        } else {
            val fileId = existingFiles[0].id
            driveService.files().get(fileId).executeMediaAsInputStream()
        }
    }

    private fun getDriveService(accessToken: String): Drive {
        return Drive.Builder(httpTransport, jsonFactory) { request ->
            request.headers.authorization = "Bearer $accessToken"
        }.setApplicationName("Hifz Companion").build()
    }
}
