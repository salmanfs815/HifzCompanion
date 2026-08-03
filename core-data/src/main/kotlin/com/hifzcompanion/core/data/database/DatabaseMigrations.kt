package com.hifzcompanion.core.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_pages ADD COLUMN stage TEXT NOT NULL DEFAULT 'NEW'")
        db.execSQL("ALTER TABLE user_pages ADD COLUMN dateMemorized INTEGER")
        db.execSQL("ALTER TABLE user_pages ADD COLUMN consecutiveStrongReviews INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_pages ADD COLUMN active INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_pages ADD COLUMN notes TEXT")
        db.execSQL("ALTER TABLE review_events ADD COLUMN reviewType TEXT NOT NULL DEFAULT 'SCHEDULED'")
        db.execSQL("ALTER TABLE review_events ADD COLUMN durationSeconds INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE review_events ADD COLUMN notes TEXT")
    }
}
