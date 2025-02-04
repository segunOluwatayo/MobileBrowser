package com.example.mobilebrowser.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.mobilebrowser.browser.GeckoSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class PendingDownload(
    val filename: String,
    val mimeType: String,
    val sourceUrl: String,
    val contentDisposition: String?
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: GeckoSessionManager
) : ViewModel() {

    private val _pendingDownload = MutableStateFlow<PendingDownload?>(null)
    val pendingDownload: StateFlow<PendingDownload?> = _pendingDownload.asStateFlow()

    fun setPendingDownload(
        filename: String,
        mimeType: String,
        sourceUrl: String,
        contentDisposition: String?
    ) {
        _pendingDownload.value = PendingDownload(
            filename = filename,
            mimeType = mimeType,
            sourceUrl = sourceUrl,
            contentDisposition = contentDisposition
        )
    }

    fun clearPendingDownload() {
        _pendingDownload.value = null
    }

    fun confirmDownload(pending: PendingDownload) {
        // Start the download using GeckoSessionManager
        sessionManager.initiateDownload(
            filename = pending.filename,
            mimeType = pending.mimeType,
            sourceUrl = pending.sourceUrl,
            contentDisposition = pending.contentDisposition
        )
        clearPendingDownload()
    }
}