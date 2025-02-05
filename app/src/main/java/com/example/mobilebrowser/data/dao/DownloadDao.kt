package com.example.mobilebrowser.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.mobilebrowser.data.entity.DownloadEntity
import com.example.mobilebrowser.data.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow


@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY downloadDate DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY downloadDate DESC")
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE filename = :filename AND fileSize = :fileSize LIMIT 1)")
    suspend fun doesFileExist(filename: String, fileSize: Long): Boolean

    @Insert
    suspend fun insertDownload(download: DownloadEntity): Long

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :downloadId")
    suspend fun deleteDownloadById(downloadId: Long)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Query("UPDATE downloads SET filename = :newFilename WHERE id = :downloadId")
    suspend fun updateFilename(downloadId: Long, newFilename: String)

    @Query("UPDATE downloads SET status = :status WHERE id = :downloadId")
    suspend fun updateStatus(downloadId: Long, status: DownloadStatus)

    @Query("SELECT * FROM downloads WHERE androidDownloadId = :androidId LIMIT 1")
    suspend fun getDownloadByAndroidId(androidId: Long): DownloadEntity?

    @Query("UPDATE downloads SET status = :status WHERE androidDownloadId = :androidId")
    suspend fun updateStatusByAndroidId(androidId: Long, status: DownloadStatus)

}