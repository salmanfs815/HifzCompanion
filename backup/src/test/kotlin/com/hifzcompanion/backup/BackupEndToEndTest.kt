package com.hifzcompanion.backup

import com.hifzcompanion.backup.drive.BackupStorage
import com.hifzcompanion.core.domain.models.BackupSnapshot
import com.hifzcompanion.core.domain.models.UserSettings
import com.hifzcompanion.core.domain.repository.BackupRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

/** Exercises the complete app boundary: snapshot -> upload -> download -> decode -> restore. */
class BackupEndToEndTest {
    private val snapshot = BackupSnapshot(
        timestamp = 1_754_000_000_000,
        quranPages = emptyList(),
        userPages = listOf("{\"pageId\":42}"),
        reviewEvents = listOf("{\"pageId\":42,\"result\":\"STRONG\"}"),
        schedulerStates = emptyList(),
        userSettingsEntities = emptyList(),
        userSettingsDataStore = UserSettings(onboardingCompleted = true, dailyCapacity = 24)
    )

    @Test
    fun backupFromOneInstallationRestoresIntoAnother() = runTest {
        val storage = InMemoryBackupStorage()
        val source = FakeBackupRepository(snapshot)
        val destination = FakeBackupRepository(snapshot.copy(timestamp = 0))

        BackupManager(source, storage).performBackup("access-token").getOrThrow()
        BackupManager(destination, storage).performRestore("new-access-token").getOrThrow()

        assertEquals(snapshot, destination.restored)
        assertTrue(storage.text().startsWith("{"))
        assertTrue(storage.text().contains("\"version\":2"))
    }

    @Test
    fun malformedBackupDoesNotModifyLocalData() = runTest {
        val storage = InMemoryBackupStorage().apply { bytes = "not valid json".toByteArray() }
        val destination = FakeBackupRepository(snapshot)

        val result = BackupManager(destination, storage).performRestore("token")

        assertTrue(result.isFailure)
        assertNull(destination.restored)
    }

    @Test
    fun missingBackupReturnsClearFailure() = runTest {
        val result = BackupManager(FakeBackupRepository(snapshot), InMemoryBackupStorage())
            .performRestore("token")

        assertTrue(result.isFailure)
        assertEquals("Backup not found", result.exceptionOrNull()?.message)
    }
}

private class FakeBackupRepository(private val snapshot: BackupSnapshot) : BackupRepository {
    var restored: BackupSnapshot? = null
    override suspend fun createSnapshot(): BackupSnapshot = snapshot
    override suspend fun restoreSnapshot(snapshot: BackupSnapshot) { restored = snapshot }
}

private class InMemoryBackupStorage : BackupStorage {
    var bytes: ByteArray? = null

    fun text(): String = bytes?.toString(Charsets.UTF_8).orEmpty()

    override suspend fun uploadBackup(accessToken: String, inputStream: InputStream, fileName: String): String {
        bytes = inputStream.readBytes()
        return "in-memory-file"
    }

    override suspend fun downloadBackup(accessToken: String, fileName: String): InputStream? =
        bytes?.let(::ByteArrayInputStream)
}
