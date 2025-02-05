package com.example.mobilebrowser.data.repository

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.mobilebrowser.data.dao.DownloadDao
import com.example.mobilebrowser.data.entity.DownloadEntity
import com.example.mobilebrowser.data.entity.DownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao
) {
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
            mimeType = mimeType
        )

        // Initialize DownloadManager request
        val request = DownloadManager.Request(Uri.parse(sourceUrl))
            .setTitle(fileName)
            .setDescription("Downloading")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(File(localPath)))
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val enqueueId = downloadManager.enqueue(request) // Get DownloadManager enqueue ID

        // Update DownloadEntity with DownloadManager enqueue ID (if needed, for tracking later)
        // For now, we'll primarily use database ID for tracking.

        return downloadDao.insertDownload(download)
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