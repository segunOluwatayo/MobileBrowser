package com.example.mobilebrowser.data.dao

import androidx.room.*
import com.example.mobilebrowser.data.entity.DownloadEntity
import com.example.mobilebrowser.data.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    // Get all downloads ordered by date added
    @Query("SELECT * FROM downloads ORDER BY dateAdded DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    // Get downloads by status
    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY dateAdded DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>>

    // Insert a new download
    @Insert
    suspend fun insertDownload(download: DownloadEntity): Long

    // Update an existing download
    @Update
    suspend fun updateDownload(download: DownloadEntity)

    // Delete a download
    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    // Check if a file is already downloaded
    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE fileName = :fileName)")
    suspend fun isFileDownloaded(fileName: String): Boolean

    // Get a specific download by ID
    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    // Update download status
    @Query("UPDATE downloads SET status = :status WHERE id = :id")
    suspend fun updateDownloadStatus(id: Long, status: DownloadStatus)

    // Update download progress
    @Query("""
        UPDATE downloads 
        SET status = :status, 
            dateCompleted = CASE WHEN :status = 'COMPLETED' THEN CURRENT_TIMESTAMP ELSE dateCompleted END 
        WHERE id = :id
    """)
    suspend fun updateDownloadProgress(id: Long, status: DownloadStatus)

    // Get download by file name
    @Query("SELECT * FROM downloads WHERE fileName = :fileName LIMIT 1")
    suspend fun getDownloadByFileName(fileName: String): DownloadEntity?
}