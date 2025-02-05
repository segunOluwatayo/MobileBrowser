package com.example.mobilebrowser.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.mobilebrowser.data.dao.DownloadDao
import com.example.mobilebrowser.data.entity.DownloadEntity
import com.example.mobilebrowser.data.entity.DownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    // Get all downloads
    fun getAllDownloads(): Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()

    // Get downloads by status
    fun getDownloadsByStatus(status: DownloadStatus): Flow<List<DownloadEntity>> =
        downloadDao.getDownloadsByStatus(status)

    // Start a new download
    suspend fun startDownload(
        fileName: String,
        sourceUrl: String,
        mimeType: String,
        fileSize: Long
    ): Long {
        val downloadDir = getDownloadDirectory()
        val localPath = File(downloadDir, fileName).absolutePath

        val download = DownloadEntity(
            fileName = fileName,
            sourceUrl = sourceUrl,
            localPath = localPath,
            fileSize = fileSize,
            status = DownloadStatus.PENDING,
            mimeType = mimeType,
            metadata = null //

        )

        // Initialize DownloadManager request
        val request = DownloadManager.Request(Uri.parse(sourceUrl))
            .setTitle(fileName)
            .setDescription("Downloading")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(File(localPath)))
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        // Enqueue the download and get the ID from DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // Store the DownloadManager ID in metadata
        val downloadWithId = download.copy(metadata = downloadId.toString())
        return downloadDao.insertDownload(downloadWithId)
    }

    // Add method to check download status
    fun getDownloadProgress(downloadManagerId: Long): Int {
        val query = DownloadManager.Query().setFilterById(downloadManagerId)

        downloadManager.query(query).use { cursor ->
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                return when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> 100
                    DownloadManager.STATUS_FAILED -> -1
                    else -> if (bytesTotal > 0) ((bytesDownloaded * 100) / bytesTotal).toInt() else 0
                }
            }
        }
        return 0
    }

    // Add method to monitor download status
    suspend fun monitorDownload(id: Long) {
        val download = downloadDao.getDownloadById(id) ?: return
        val downloadManagerId = download.metadata?.toLongOrNull() ?: return

        var lastProgress = -1
        while (true) {
            val query = DownloadManager.Query().setFilterById(downloadManagerId)
            downloadManager.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    val progress = if (bytesTotal > 0) ((bytesDownloaded * 100) / bytesTotal).toInt() else 0

                    if (progress != lastProgress) {
                        lastProgress = progress
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                downloadDao.updateDownloadStatus(id, DownloadStatus.COMPLETED)
                                return
                            }
                            DownloadManager.STATUS_FAILED -> {
                                downloadDao.updateDownloadStatus(id, DownloadStatus.FAILED)
                                return
                            }
                            DownloadManager.STATUS_RUNNING -> {
                                downloadDao.updateDownloadStatus(id, DownloadStatus.IN_PROGRESS)
                            }
                        }
                    }
                }
            }
            delay(500) // Check every half second
        }
    }
    // Update download status
    suspend fun updateDownloadStatus(id: Long, status: DownloadStatus) {
        downloadDao.updateDownloadStatus(id, status)
    }

    // Check if file is already downloaded
    suspend fun isFileDownloaded(fileName: String): Boolean =
        downloadDao.isFileDownloaded(fileName)

    // Get download by ID
    suspend fun getDownloadById(id: Long): DownloadEntity? =
        downloadDao.getDownloadById(id)

    // Delete download and its file
    suspend fun deleteDownload(download: DownloadEntity) {
        val file = File(download.localPath)
        if (file.exists()) {
            file.delete()
        }
        downloadDao.deleteDownload(download)
    }

    // Rename downloaded file
    suspend fun renameDownload(download: DownloadEntity, newFileName: String): Boolean {
        val oldFile = File(download.localPath)
        if (!oldFile.exists()) return false

        val newFile = File(oldFile.parent, newFileName)
        return try {
            if (oldFile.renameTo(newFile)) {
                val updatedDownload = download.copy(
                    fileName = newFileName,
                    localPath = newFile.absolutePath
                )
                downloadDao.updateDownload(updatedDownload)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // Get download directory
    private fun getDownloadDirectory(): File {
        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "MobileBrowser"
        )
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        return downloadDir
    }

    // Get download by file name
    suspend fun getDownloadByFileName(fileName: String): DownloadEntity? =
        downloadDao.getDownloadByFileName(fileName)
}