package com.example.mobilebrowser.ui.viewmodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.entity.DownloadEntity
import com.example.mobilebrowser.data.repository.DownloadRepository
import com.example.mobilebrowser.data.util.PermissionsHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val repository: DownloadRepository,
    @ApplicationContext private val context: Context,
    private val permissionsHandler: PermissionsHandler
) : ViewModel() {

    // Existing download list state
    private val _downloads = MutableStateFlow<List<DownloadEntity>>(emptyList())
    val downloads: StateFlow<List<DownloadEntity>> = repository.getAllDownloads()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Temporary deletion state
    private val _recentlyDeletedDownload = MutableStateFlow<DownloadEntity?>(null)
    val recentlyDeletedDownload: StateFlow<DownloadEntity?> = _recentlyDeletedDownload.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Download state for tracking various stages
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    // Download progress tracking (downloadId to progress percentage)
    private val _downloadProgress = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<Long, Int>> = _downloadProgress.asStateFlow()

    // Permissions state
    private val _hasPermissions = MutableStateFlow(permissionsHandler.hasStoragePermissions())
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    init {
        // Monitor permissions changes (assuming permissionsHandler exposes a Flow<Boolean>)
        viewModelScope.launch {
            permissionsHandler.permissionsGranted.asFlow().collect { granted ->
                _hasPermissions.value = granted
            }
        }
    }

    // Expose permissions helper functions
    fun checkPermissions() {
        _hasPermissions.value = permissionsHandler.hasStoragePermissions()
    }

    fun getPermissionRationale(): String = permissionsHandler.getStoragePermissionRationale()

    fun requiredPermissions(): List<String> = permissionsHandler.requiredPermissions()

    // Updated startDownload with progress tracking and permission checking
    fun startDownload(
        filename: String,
        mimeType: String,
        fileSize: Long,
        sourceUrl: String,
        contentDisposition: String? = null
    ) {
        viewModelScope.launch {
            try {
                if (!hasPermissions.value) {
                    _downloadState.value = DownloadState.NoPermission
                    return@launch
                }

                // Check if the file already exists
                val fileExists = repository.doesFileExist(filename, fileSize)
                if (fileExists) {
                    _downloadState.value = DownloadState.FileExists(filename)
                    return@launch
                }

                // Create the download and get the download ID
                val downloadId = repository.createDownload(
                    filename = filename,
                    mimeType = mimeType,
                    fileSize = fileSize,
                    sourceUrl = sourceUrl,
                    contentDisposition = contentDisposition
                )

                _downloadState.value = DownloadState.Started(downloadId)

                // Monitor download progress (assumes repository.getDownloadProgress returns a Flow<Int>)
                repository.getDownloadProgress(downloadId).collect { progress ->
                    _downloadProgress.update { current ->
                        current.toMutableMap().apply {
                            put(downloadId, progress)
                        }
                    }
                    if (progress == 100) {
                        _downloadState.value = DownloadState.Completed(filename, mimeType)
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to start download: ${e.message}"
                _downloadState.value = DownloadState.Failed(e.message ?: "Unknown error")
            }
        }
    }

    // Enhanced file type handling for opening downloaded files
    fun openDownloadedFile(downloadId: Long) {
        viewModelScope.launch {
            try {
                repository.getDownloadForSharing(downloadId)?.let { download ->
                    val intent = when {
                        // Audio files
                        download.mimeType.startsWith("audio/") -> {
                            Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(
                                    Uri.parse("content://media/external/audio/media"),
                                    "audio/*"
                                )
                            }
                        }
                        // Video files
                        download.mimeType.startsWith("video/") -> {
                            createFileIntent(download, "video/*")
                        }
                        // Image files
                        download.mimeType.startsWith("image/") -> {
                            createFileIntent(download, "image/*")
                        }
                        // Documents and other application types
                        download.mimeType.startsWith("application/") -> {
                            when {
                                download.mimeType.endsWith("pdf") ->
                                    createFileIntent(download, "application/pdf")
                                download.mimeType.endsWith("msword") ||
                                        download.mimeType.endsWith("document") ->
                                    createFileIntent(download, "application/msword")
                                download.mimeType.endsWith("spreadsheet") ->
                                    createFileIntent(download, "application/vnd.ms-excel")
                                download.mimeType.endsWith("presentation") ->
                                    createFileIntent(download, "application/vnd.ms-powerpoint")
                                else ->
                                    createFileIntent(download, download.mimeType)
                            }
                        }
                        else -> createFileIntent(download, download.mimeType)
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                _error.value = "Failed to open file: ${e.message}"
            }
        }
    }

    // Helper function to create an Intent for a file using FileProvider
    private fun createFileIntent(download: DownloadEntity, mimeType: String): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(download.path)
            )
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
                        delay(7000)
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
}

// New DownloadState sealed class with various states
sealed class DownloadState {
    object Idle : DownloadState()
    object NoPermission : DownloadState()
    data class Started(val downloadId: Long) : DownloadState()
    data class FileExists(val filename: String) : DownloadState()
    data class Completed(val filename: String, val mimeType: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}
