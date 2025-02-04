package com.example.mobilebrowser.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.entity.DownloadEntity
import com.example.mobilebrowser.data.entity.DownloadStatus
import com.example.mobilebrowser.data.repository.DownloadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext


@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val repository: DownloadRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    // UI State for downloads
    private val _downloads = MutableStateFlow<List<DownloadEntity>>(emptyList())
    val downloads: StateFlow<List<DownloadEntity>> = repository.getAllDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // State for temporary deletion
    private val _recentlyDeletedDownload = MutableStateFlow<DownloadEntity?>(null)
    val recentlyDeletedDownload: StateFlow<DownloadEntity?> = _recentlyDeletedDownload.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Check if file exists
    suspend fun checkFileExists(filename: String, fileSize: Long): Boolean =
        repository.doesFileExist(filename, fileSize)

    // Function to open downloaded file
    fun openDownloadedFile(downloadId: Long) {
        viewModelScope.launch {
            try {
                repository.getDownloadForSharing(downloadId)?.let { download ->
                    val intent = when {
                        download.mimeType.startsWith("audio/") -> {
                            Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(
                                    Uri.parse("content://media/external/audio/media"),
                                    "audio/*"
                                )
                            }
                        }
                        else -> {
                            Intent(Intent.ACTION_VIEW).apply {
                                val uri = FileProvider.getUriForFile(
                                    context,  // now available
                                    "${context.packageName}.fileprovider",
                                    File(download.path)
                                )
                                setDataAndType(uri, download.mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        }
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                _error.value = "Failed to open file: ${e.message}"
            }
        }
    }


    // Start new download
    fun startDownload(
        filename: String,
        mimeType: String,
        fileSize: Long,
        sourceUrl: String,
        contentDisposition: String? = null
    ) {
        viewModelScope.launch {
            try {
                repository.createDownload(
                    filename = filename,
                    mimeType = mimeType,
                    fileSize = fileSize,
                    sourceUrl = sourceUrl,
                    contentDisposition = contentDisposition
                )
            } catch (e: Exception) {
                _error.value = "Failed to start download: ${e.message}"
            }
        }
    }

    // Rename download
    fun renameDownload(downloadId: Long, newFilename: String) {
        viewModelScope.launch {
            try {
                repository.renameDownload(downloadId, newFilename)
            } catch (e: Exception) {
                _error.value = "Failed to rename file: ${e.message}"
            }
        }
    }

    // Delete download with undo capability
    fun deleteDownload(downloadId: Long) {
        viewModelScope.launch {
            try {
                val download = repository.getDownloadById(downloadId)
                if (download != null) {
                    _recentlyDeletedDownload.value = download
                    repository.deleteDownload(downloadId)

                    // Auto-dismiss after 7 seconds if not undone
                    launch {
                        kotlinx.coroutines.delay(7000)
                        if (_recentlyDeletedDownload.value?.id == downloadId) {
                            _recentlyDeletedDownload.value = null
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete file: ${e.message}"
            }
        }
    }

    // Undo delete
    fun undoDelete() {
        viewModelScope.launch {
            _recentlyDeletedDownload.value?.let { download ->
                try {
                    repository.createDownload(
                        filename = download.filename,
                        mimeType = download.mimeType,
                        fileSize = download.fileSize,
                        sourceUrl = download.sourceUrl,
                        contentDisposition = download.contentDisposition
                    )
                    _recentlyDeletedDownload.value = null
                } catch (e: Exception) {
                    _error.value = "Failed to restore file: ${e.message}"
                }
            }
        }
    }

    // Share download
    suspend fun shareDownload(downloadId: Long): Intent? {
        return repository.getDownloadForSharing(downloadId)?.let { download ->
            Intent().apply {
                action = Intent.ACTION_SEND
                type = download.mimeType
                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(download.path)))
            }
        }
    }

    // Clear error
    fun clearError() {
        _error.value = null
    }

    sealed class DownloadState {
        object Idle : DownloadState()
        object Started : DownloadState()
        data class FileExists(val filename: String) : DownloadState()
        data class Completed(val filename: String) : DownloadState()
    }
}