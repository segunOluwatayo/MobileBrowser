package com.example.mobilebrowser.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.entity.DownloadEntity
import com.example.mobilebrowser.data.entity.DownloadStatus
import com.example.mobilebrowser.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val repository: DownloadRepository
) : ViewModel() {
    private val _downloadProgress = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<Long, Int>> = _downloadProgress.asStateFlow()


    // Stream of all downloads
    val downloads = repository.getAllDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Recently deleted download for undo functionality
    private var recentlyDeletedDownload: DownloadEntity? = null

    // Start a new download
    suspend fun startDownload(fileName: String, url: String, mimeType: String, fileSize: Long): Long {
        Log.d("DownloadViewModel", "Starting download: $fileName")
        val downloadId = repository.startDownload(fileName, url, mimeType, fileSize)
        Log.d("DownloadViewModel", "Download started with ID: $downloadId")

        // Start monitoring the download status
        viewModelScope.launch {
            repository.monitorDownload(downloadId)
        }

        return downloadId
    }


    // Added method to show completion dialog only when download is actually complete
    // Check download status
    fun shouldShowCompletionDialog(downloadId: Long): Flow<Boolean> {
        return downloads
            .map { downloadList ->
                val download = downloadList.find { it.id == downloadId }
                Log.d("DownloadViewModel", "Checking download status for ID $downloadId: ${download?.status}")
                download?.status == DownloadStatus.COMPLETED
            }
    }

    // Check if file is already downloaded
    suspend fun isFileDownloaded(fileName: String): Boolean =
        repository.isFileDownloaded(fileName)

    // Delete download
    fun deleteDownload(download: DownloadEntity) {
        viewModelScope.launch {
            recentlyDeletedDownload = download
            repository.deleteDownload(download)
        }
    }

    // Undo recent deletion
    fun undoDelete() {
        viewModelScope.launch {
            recentlyDeletedDownload?.let { download ->
                val newDownloadId = repository.startDownload(
                    download.fileName,
                    download.sourceUrl,
                    download.mimeType,
                    download.fileSize
                )
                if (newDownloadId > 0) {
                    repository.updateDownloadStatus(newDownloadId, DownloadStatus.COMPLETED)
                }
                recentlyDeletedDownload = null
            }
        }
    }

    // Rename download
    suspend fun renameDownload(download: DownloadEntity, newFileName: String): Boolean =
        repository.renameDownload(download, newFileName)
}