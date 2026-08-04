package com.hifzcompanion.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hifzcompanion.core.data.entities.QuranPageEntity
import com.hifzcompanion.core.data.entities.ReviewEventEntity
import com.hifzcompanion.core.data.entities.SchedulerStateEntity
import com.hifzcompanion.core.data.entities.UserPageEntity
import com.hifzcompanion.core.data.entities.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HifzDao {
    @Query("SELECT * FROM quran_pages")
    fun getAllQuranPages(): Flow<List<QuranPageEntity>>

    @Query("SELECT * FROM user_pages")
    fun getAllUserPages(): Flow<List<UserPageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPage(userPage: UserPageEntity)

    @Update
    suspend fun updateUserPage(userPage: UserPageEntity)

    @Insert
    suspend fun insertReviewEvent(event: ReviewEventEntity)

    @Query("SELECT * FROM review_events WHERE pageId = :pageId")
    fun getReviewEventsForPage(pageId: Int): Flow<List<ReviewEventEntity>>

    @Query("SELECT * FROM review_events WHERE pageId = :pageId ORDER BY timestamp, id")
    suspend fun getReviewEventsForPageSnapshot(pageId: Int): List<ReviewEventEntity>

    @Query("UPDATE review_events SET result = :result, difficulty = :difficulty WHERE id = :eventId")
    suspend fun updateReviewEventQuality(eventId: Int, result: String, difficulty: Int)

    @Query("SELECT * FROM review_events ORDER BY timestamp DESC, id DESC")
    fun getAllReviewEvents(): Flow<List<ReviewEventEntity>>

    @Query("SELECT * FROM user_pages WHERE pageId = :pageId LIMIT 1")
    fun getUserPage(pageId: Int): Flow<UserPageEntity?>

    @Query("SELECT * FROM quran_pages")
    suspend fun getAllQuranPagesSnapshot(): List<QuranPageEntity>

    @Query("SELECT * FROM user_pages")
    suspend fun getAllUserPagesSnapshot(): List<UserPageEntity>

    @Query("SELECT * FROM review_events")
    suspend fun getAllReviewEventsSnapshot(): List<ReviewEventEntity>

    @Query("SELECT * FROM scheduler_state")
    suspend fun getAllSchedulerStateSnapshot(): List<SchedulerStateEntity>

    @Query("SELECT * FROM user_settings")
    suspend fun getAllUserSettingsSnapshot(): List<UserSettingsEntity>

    @Query("DELETE FROM user_pages")
    suspend fun deleteAllUserPages()

    @Query("DELETE FROM review_events")
    suspend fun deleteAllReviewEvents()

    @Query("DELETE FROM scheduler_state")
    suspend fun deleteAllSchedulerState()

    @Query("DELETE FROM user_settings")
    suspend fun deleteAllUserSettings()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPages(pages: List<UserPageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewEvents(events: List<ReviewEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedulerStates(states: List<SchedulerStateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSettings(settings: List<UserSettingsEntity>)

    @androidx.room.Transaction
    suspend fun replaceBackupData(
        pages: List<UserPageEntity>,
        events: List<ReviewEventEntity>,
        states: List<SchedulerStateEntity>,
        settings: List<UserSettingsEntity>
    ) {
        deleteAllUserPages()
        deleteAllReviewEvents()
        deleteAllSchedulerState()
        deleteAllUserSettings()
        insertUserPages(pages)
        insertReviewEvents(events)
        insertSchedulerStates(states)
        insertUserSettings(settings)
    }

    @Query("SELECT COUNT(DISTINCT pageId) FROM review_events WHERE timestamp >= :since")
    fun getReviewCountSince(since: Long): Flow<Int>
}
