package com.example.mobilebrowser.data.dao

import androidx.room.*
import com.example.mobilebrowser.data.entity.ShortcutEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {

    @Query("SELECT * FROM shortcuts ORDER BY isPinned DESC, timestamp DESC")
    fun getAllShortcuts(): Flow<List<ShortcutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: ShortcutEntity)

    @Update
    suspend fun updateShortcut(shortcut: ShortcutEntity)

    @Delete
    suspend fun deleteShortcut(shortcut: ShortcutEntity)

    // Optionally, add a query to get only pinned shortcuts
    @Query("SELECT * FROM shortcuts WHERE isPinned = 1 ORDER BY timestamp DESC")
    fun getPinnedShortcuts(): Flow<List<ShortcutEntity>>
}
