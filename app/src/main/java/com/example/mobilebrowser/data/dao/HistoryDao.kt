package com.example.mobilebrowser.data.dao

import androidx.room.*
import com.example.mobilebrowser.data.entity.HistoryEntity
import com.example.mobilebrowser.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface HistoryDao {
    // Retrieve all history entries, ordered by last visit date (most recent first)
    @Query("SELECT * FROM history ORDER BY lastVisited DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    // Get all history entries as a List needed for sync operations
    @Query("SELECT * FROM history ORDER BY lastVisited DESC")
    suspend fun getAllHistoryAsList(): List<HistoryEntity>

    // Search history entries by title or URL
    @Query("SELECT * FROM history WHERE title LIKE :query OR url LIKE :query ORDER BY lastVisited DESC")
    fun searchHistory(query: String): Flow<List<HistoryEntity>>

    // Insert a new history entry.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    // Update an existing history entry.
    @Update
    suspend fun updateHistory(history: HistoryEntity)

    // Delete a specific history entry.
    @Delete
    suspend fun deleteHistory(history: HistoryEntity)

    // Delete history entries within a specific time range.
    @Query("DELETE FROM history WHERE lastVisited BETWEEN :startDate AND :endDate")
    suspend fun deleteHistoryInRange(startDate: Date, endDate: Date)

    // Get history entries within a specific time range as a List
    @Query("SELECT * FROM history WHERE lastVisited BETWEEN :startDate AND :endDate ORDER BY lastVisited DESC")
    suspend fun getHistoryInRangeAsList(startDate: Date, endDate: Date): List<HistoryEntity>

    // Delete all history entries.
    @Query("DELETE FROM history")
    suspend fun deleteAllHistory()

    // Get a specific history entry by its URL.
    // Returns null if no matching entry is found.
    @Query("SELECT * FROM history WHERE url = :url LIMIT 1")
    suspend fun getHistoryByUrl(url: String): HistoryEntity?

    // Get history entries for a specific date range.
    @Query("SELECT * FROM history WHERE lastVisited BETWEEN :startDate AND :endDate ORDER BY lastVisited DESC")
    fun getHistoryInRange(startDate: Date, endDate: Date): Flow<List<HistoryEntity>>

    // Get the most recent history entries, limited by the provided count.
    @Query("SELECT * FROM history ORDER BY lastVisited DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<HistoryEntity>>

    // Retrieve history entries filtered by their synchronization status.
    @Query("SELECT * FROM history WHERE syncStatus = :status")
    fun getHistoryBySyncStatus(status: SyncStatus): Flow<List<HistoryEntity>>
}