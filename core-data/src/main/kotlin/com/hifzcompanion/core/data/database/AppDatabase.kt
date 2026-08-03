package com.hifzcompanion.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hifzcompanion.core.data.dao.HifzDao
import com.hifzcompanion.core.data.entities.QuranPageEntity
import com.hifzcompanion.core.data.entities.ReviewEventEntity
import com.hifzcompanion.core.data.entities.SchedulerStateEntity
import com.hifzcompanion.core.data.entities.UserPageEntity
import com.hifzcompanion.core.data.entities.UserSettingsEntity

@Database(
    entities = [
        QuranPageEntity::class,
        UserPageEntity::class,
        ReviewEventEntity::class,
        SchedulerStateEntity::class,
        UserSettingsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hifzDao(): HifzDao
}
