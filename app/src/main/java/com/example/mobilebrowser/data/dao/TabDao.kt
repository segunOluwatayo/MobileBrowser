package com.example.mobilebrowser.data.dao

import androidx.room.*
import com.example.mobilebrowser.data.entity.TabEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for tab-related database operations.
 */
@Dao
interface TabDao {
    /**
     * Retrieves all tabs ordered by their position
     */
    @Query("SELECT * FROM tabs ORDER BY position ASC")
    fun getAllTabs(): Flow<List<TabEntity>>

    /**
     * Retrieves the currently active tab
     */
    @Query("SELECT * FROM tabs WHERE isActive = 1 LIMIT 1")
    fun getActiveTab(): Flow<TabEntity?>

    /**
     * Retrieves a specific tab by its ID
     */
    @Query("SELECT * FROM tabs WHERE id = :tabId")
    suspend fun getTabById(tabId: Long): TabEntity?

    /**
     * Inserts a new tab into the database
     */
    @Insert
    suspend fun insertTab(tab: TabEntity): Long

    /**
     * Updates an existing tab
     */
    @Update
    suspend fun updateTab(tab: TabEntity)

    /**
     * Deletes a specific tab
     */
    @Delete
    suspend fun deleteTab(tab: TabEntity)

    /**
     * Deletes all tabs
     */
    @Query("DELETE FROM tabs")
    suspend fun deleteAllTabs()

    /**
     * Sets all tabs as inactive
     */
    @Query("UPDATE tabs SET isActive = 0")
    suspend fun deactivateAllTabs()

    /**
     * Sets a specific tab as active
     */
    @Query("UPDATE tabs SET isActive = 1 WHERE id = :tabId")
    suspend fun setTabActive(tabId: Long)

    /**
     * Updates the position of a tab
     */
    @Query("UPDATE tabs SET position = :newPosition WHERE id = :tabId")
    suspend fun updateTabPosition(tabId: Long, newPosition: Int)

    /**
     * Gets the count of open tabs
     */
    @Query("SELECT COUNT(*) FROM tabs")
    fun getTabCount(): Flow<Int>
}