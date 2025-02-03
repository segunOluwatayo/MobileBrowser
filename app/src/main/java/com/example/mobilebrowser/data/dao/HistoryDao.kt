package com.example.mobilebrowser.data.dao

import androidx.room.*
import com.example.mobilebrowser.data.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface HistoryDao {
    // Retrieve all history entries, ordered by last visit date (most recent first)
    @Query("SELECT * FROM history ORDER BY lastVisited DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    // Search history entries by title or URL
    @Query("SELECT * FROM history WHERE title LIKE :query OR url LIKE :query ORDER BY lastVisited DESC")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    // Insert a new history entry
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    // Delete a specific history entry
    @Delete
    suspend fun deleteHistory(history: HistoryEntity)

    // Delete history entries within a specific time range
    @Query("DELETE FROM history WHERE lastVisited BETWEEN :startDate AND :endDate")
    suspend fun deleteHistoryInRange(startDate: Date, endDate: Date)

    // Delete all history
    @Query("DELETE FROM history")
    suspend fun deleteAllHistory()

    // Update visit count and last visited date for an existing URL
    @Query("""
        UPDATE history 
        SET visitCount = visitCount + 1, 
            lastVisited = :lastVisited 
        WHERE url = :url
    """)
    suspend fun incrementVisitCount(url: String, lastVisited: Date = Date())

    // Check if a URL exists in history
    @Query("SELECT EXISTS(SELECT 1 FROM history WHERE url = :url)")
    suspend fun isUrlInHistory(url: String): Boolean

    // Get history entries for a specific date range
    @Query("SELECT * FROM history WHERE lastVisited BETWEEN :startDate AND :endDate ORDER BY lastVisited DESC")
    fun getHistoryInRange(startDate: Date, endDate: Date): Flow<List<HistoryEntity>>

    // Get history entry by URL
    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun getHistoryByUrl(url: String): HistoryEntity?
}