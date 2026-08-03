package com.hifzcompanion.core.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "quran_pages")
@Serializable
data class QuranPageEntity(
    @PrimaryKey val id: Int,
    val surahName: String,
    val surahNumber: Int,
    val pageNumber: Int,
    val juzNumber: Int
)
