package com.hifzcompanion.core.data.di

import android.content.Context
import androidx.room.Room
import com.hifzcompanion.core.data.dao.HifzDao
import com.hifzcompanion.core.data.database.AppDatabase
import com.hifzcompanion.core.data.database.MIGRATION_1_2
import com.hifzcompanion.core.data.repository.BackupRepositoryImpl
import com.hifzcompanion.core.data.repository.HifzRepositoryImpl
import com.hifzcompanion.core.domain.repository.BackupRepository
import com.hifzcompanion.core.domain.repository.HifzRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    @Binds
    @Singleton
    abstract fun bindHifzRepository(impl: HifzRepositoryImpl): HifzRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "hifz_companion.db"
            ).addMigrations(MIGRATION_1_2).build()
        }

        @Provides
        fun provideHifzDao(database: AppDatabase): HifzDao {
            return database.hifzDao()
        }

        @Provides
        @Singleton
        fun provideHifzScheduler(): com.hifzcompanion.scheduler.HifzScheduler {
            return com.hifzcompanion.scheduler.HifzScheduler()
        }
    }
}
