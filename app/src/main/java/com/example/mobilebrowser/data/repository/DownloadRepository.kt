package com.example.mobilebrowser.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.example.mobilebrowser.data.dao.DownloadDao
import com.example.mobilebrowser.data.entity.DownloadEntity
import com.example.mobilebrowser.data.entity.DownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
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
        androidDownloadId: Long,
        contentDisposition: String? = null
    ): Long {
        val download = DownloadEntity(
            filename = filename,
            path = getDownloadPath(filename),
            mimeType = mimeType,
            fileSize = fileSize,
            status = DownloadStatus.IN_PROGRESS,
            sourceUrl = sourceUrl,
            androidDownloadId = androidDownloadId,
            contentDisposition = contentDisposition
        )
        return downloadDao.insertDownload(download)
    }

    // Update download status
//    suspend fun updateDownloadStatus(downloadId: Long, status: DownloadStatus) {
//        downloadDao.updateStatus(downloadId, status)
//    }
    suspend fun updateDownloadStatusByAndroidId(androidId: Long, status: DownloadStatus) {
        downloadDao.updateStatusByAndroidId(androidId, status)
    }

    suspend fun getDownloadByAndroidId(androidId: Long): DownloadEntity? {
        return downloadDao.getDownloadByAndroidId(androidId)
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
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        val fullPath = "$downloadsDir/$filename"
        Log.d("DownloadRepository", "Computed download path: $fullPath")
        return fullPath
    }

    fun getDownloadProgress(downloadId: Long): Flow<Int> = flow {
        var progress = 0
        // Emit progress updates every 500ms until reaching 100
        while (progress < 100) {
            delay(500)
            progress += 10
            emit(progress)
        }
    }
}