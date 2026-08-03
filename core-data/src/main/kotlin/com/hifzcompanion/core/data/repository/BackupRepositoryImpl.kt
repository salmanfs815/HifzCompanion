package com.hifzcompanion.core.data.repository

import com.hifzcompanion.core.data.dao.HifzDao
import com.hifzcompanion.core.domain.models.BackupSnapshot
import com.hifzcompanion.core.domain.repository.BackupRepository
import com.hifzcompanion.core.domain.repository.UserSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class BackupRepositoryImpl @Inject constructor(
    private val hifzDao: HifzDao,
    private val userSettingsRepository: UserSettingsRepository
) : BackupRepository {

    override suspend fun createSnapshot(): BackupSnapshot {
        val quranPages = hifzDao.getAllQuranPagesSnapshot()
        val userPages = hifzDao.getAllUserPagesSnapshot()
        val reviewEvents = hifzDao.getAllReviewEventsSnapshot()
        val schedulerStates = hifzDao.getAllSchedulerStateSnapshot()
        val userSettingsEntities = hifzDao.getAllUserSettingsSnapshot()
        val userSettingsDataStore = userSettingsRepository.getUserSettings().first()

        return BackupSnapshot(
            timestamp = System.currentTimeMillis(),
            quranPages = quranPages.map { Json.encodeToString(it) },
            userPages = userPages.map { Json.encodeToString(it) },
            reviewEvents = reviewEvents.map { Json.encodeToString(it) },
            schedulerStates = schedulerStates.map { Json.encodeToString(it) },
            userSettingsEntities = userSettingsEntities.map { Json.encodeToString(it) },
            userSettingsDataStore = userSettingsDataStore
        )
    }

    override suspend fun restoreSnapshot(snapshot: BackupSnapshot) {
        require(snapshot.version in 1..2) { "Unsupported backup version ${snapshot.version}" }
        hifzDao.replaceBackupData(
            pages = snapshot.userPages.map { Json.decodeFromString(it) },
            events = snapshot.reviewEvents.map { Json.decodeFromString(it) },
            states = snapshot.schedulerStates.map { Json.decodeFromString(it) },
            settings = snapshot.userSettingsEntities.map { Json.decodeFromString(it) }
        )
        userSettingsRepository.updateUserSettings(snapshot.userSettingsDataStore)
    }
}
