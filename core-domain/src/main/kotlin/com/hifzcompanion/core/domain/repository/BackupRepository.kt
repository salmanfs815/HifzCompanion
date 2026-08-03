package com.hifzcompanion.core.domain.repository

import com.hifzcompanion.core.domain.models.BackupSnapshot

interface BackupRepository {
    suspend fun createSnapshot(): BackupSnapshot
    suspend fun restoreSnapshot(snapshot: BackupSnapshot)
}
