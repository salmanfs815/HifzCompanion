package com.hifzcompanion.backup.di

import com.hifzcompanion.backup.drive.BackupStorage
import com.hifzcompanion.backup.drive.GoogleDriveManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {
    @Binds
    @Singleton
    abstract fun bindBackupStorage(implementation: GoogleDriveManager): BackupStorage
}
