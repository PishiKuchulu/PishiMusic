package com.tuned.app.data.local.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Query
import androidx.room.Upsert

/** How much of one song was heard in one calendar year, which [SongPlayStatsEntity]'s all-time totals can't say. */
@Entity(tableName = "yearly_listens", primaryKeys = ["year", "trackKey"])
data class YearlyListenEntity(
    val year: Int,
    val trackKey: String,
    val title: String = "",
    val artist: String = "",
    val videoId: String? = null,
    val artworkUrl: String? = null,
    val listenedMs: Long = 0L,
    val plays: Int = 0,
)

@Dao
interface YearlyListenDao {
    @Query("SELECT * FROM yearly_listens WHERE year = :year AND trackKey = :trackKey LIMIT 1")
    suspend fun find(year: Int, trackKey: String): YearlyListenEntity?

    @Upsert
    suspend fun upsert(entity: YearlyListenEntity)

    @Query("SELECT * FROM yearly_listens WHERE year = :year ORDER BY listenedMs DESC")
    suspend fun forYear(year: Int): List<YearlyListenEntity>

    @Query("SELECT DISTINCT year FROM yearly_listens ORDER BY year DESC")
    suspend fun years(): List<Int>
}
