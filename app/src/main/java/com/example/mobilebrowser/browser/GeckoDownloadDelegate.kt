package com.example.mobilebrowser.browser

import android.content.Context
import android.net.Uri
import com.example.mobilebrowser.ui.viewmodels.DownloadViewModel
import com.example.mobilebrowser.ui.util.PermissionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession

class GeckoDownloadDelegate(
    private val context: Context,
    private val downloadViewModel: DownloadViewModel,
    private val scope: CoroutineScope,
    private val showDownloadConfirmation: (DownloadRequest) -> Unit
) : GeckoSession.ContentDelegate {

    data class DownloadRequest(
        val fileName: String,
        val url: String,
        val contentLength: Long,
        val contentType: String
    )

    override fun onTitleChange(session: GeckoSession, title: String?) {
        // Required override
    }

    fun onExternalResponse(session: GeckoSession, response: GeckoSession.WebResponseInfo) {
        if (!PermissionHandler.hasStoragePermission(context)) {
            // Handle permission request
            return
        }

        val downloadRequest = DownloadRequest(
            fileName = response.filename ?: Uri.parse(response.uri).lastPathSegment ?: "unknown",
            url = response.uri,
            contentLength = response.contentLength,
            contentType = response.contentType ?: "application/octet-stream"
        )

        scope.launch {
            if (downloadViewModel.isFileDownloaded(downloadRequest.fileName)) {
                // Show re-download confirmation
                showDownloadConfirmation(downloadRequest)
            } else {
                // Show initial download confirmation
                showDownloadConfirmation(downloadRequest)
            }
        }
    }
}