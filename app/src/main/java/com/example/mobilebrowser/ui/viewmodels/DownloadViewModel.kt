package com.example.mobilebrowser.ui.viewmodels

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
        return repository.startDownload(fileName, url, mimeType, fileSize)
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