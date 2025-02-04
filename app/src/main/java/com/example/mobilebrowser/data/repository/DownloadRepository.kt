package com.example.mobilebrowser.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.mobilebrowser.data.dao.DownloadDao
import com.example.mobilebrowser.data.entity.DownloadEntity
import com.example.mobilebrowser.data.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    // Get download by ID
    suspend fun getDownloadById(id: Long): DownloadEntity? = downloadDao.getDownloadById(id)

    // Get all downloads
    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    // Get downloads by status
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>> =
        downloadDao.getDownloadsByStatus(status)

    // Check if file exists
    suspend fun doesFileExist(filename: String, fileSize: Long): Boolean =
        downloadDao.doesFileExist(filename, fileSize)

    // Create new download entry
    suspend fun createDownload(
        filename: String,
        mimeType: String,
        fileSize: Long,
        sourceUrl: String,
        contentDisposition: String? = null
    ): Long {
        val download = DownloadEntity(
            filename = filename,
            path = getDownloadPath(filename),
            mimeType = mimeType,
            fileSize = fileSize,
            status = DownloadStatus.IN_PROGRESS,
            sourceUrl = sourceUrl,
            contentDisposition = contentDisposition
        )
        return downloadDao.insertDownload(download)
    }

    // Update download status
    suspend fun updateDownloadStatus(downloadId: Long, status: DownloadStatus) {
        downloadDao.updateStatus(downloadId, status)
    }

    // Rename download
    suspend fun renameDownload(downloadId: Long, newFilename: String) {
        val download = downloadDao.getDownloadById(downloadId) ?: return

        try {
            // Handle file rename based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10 and above
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, newFilename)
                }

                context.contentResolver.update(
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    contentValues,
                    "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                    arrayOf(download.filename)
                )
            } else {
                // Direct file operations for older Android versions
                val oldFile = File(download.path)
                val newFile = File(oldFile.parent, newFilename)
                if (oldFile.exists() && oldFile.renameTo(newFile)) {
                    downloadDao.updateFilename(downloadId, newFilename)
                }
            }
        } catch (e: IOException) {
            throw IOException("Failed to rename file: ${e.message}")
        }
    }

    // Delete download
    suspend fun deleteDownload(downloadId: Long) {
        val download = downloadDao.getDownloadById(downloadId) ?: return

        try {
            // Handle file deletion based on Android version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10 and above
                context.contentResolver.delete(
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                    arrayOf(download.filename)
                )
            } else {
                // Direct file operations for older Android versions
                val file = File(download.path)
                if (file.exists()) {
                    file.delete()
                }
            }

            downloadDao.deleteDownloadById(downloadId)
        } catch (e: IOException) {
            throw IOException("Failed to delete file: ${e.message}")
        }
    }

    // Share download
    suspend fun getDownloadForSharing(downloadId: Long): DownloadEntity? =
        downloadDao.getDownloadById(downloadId)

    private fun getDownloadPath(filename: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above, use MediaStore path
            "${Environment.DIRECTORY_DOWNLOADS}/$filename"
        } else {
            // For older versions, use direct file path
            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}/$filename"
        }
    }
}