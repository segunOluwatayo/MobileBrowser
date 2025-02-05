package com.example.mobilebrowser.browser

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.mobilebrowser.ui.viewmodels.DownloadViewModel
import com.example.mobilebrowser.ui.util.PermissionHandler
import com.example.mobilebrowser.util.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebResponse


class GeckoDownloadDelegate(
    private val context: Context,
    private val downloadViewModel: DownloadViewModel,
    private val scope: CoroutineScope,
    private val showDownloadConfirmation: (DownloadRequest) -> Unit // Callback function
) : GeckoSession.ContentDelegate {

    data class DownloadRequest( // Keep DownloadRequest data class
        val fileName: String,
        val url: String,
        val contentLength: Long,
        val contentType: String
    )

    override fun onTitleChange(session: GeckoSession, title: String?) {
        // Required override
    }

    override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
        Log.d("GeckoDownloadDelegate", "onExternalResponse called for URL: ${response.uri}")

        if (!PermissionHandler.hasStoragePermission(context)) {
            Log.w("GeckoDownloadDelegate", "Storage permission not granted")
            return
        }

        val fileNameFromUrl = Uri.parse(response.uri).lastPathSegment ?: "unknown"
        val safeFileName = FileUtils.getSafeFileName(fileNameFromUrl)

        val headers = response.headers
        val contentLength = headers["Content-Length"]?.toLongOrNull() ?: 0L
        val contentType = headers["Content-Type"] ?: "application/octet-stream"

        val downloadRequest = DownloadRequest(
            fileName = safeFileName,
            url = response.uri,
            contentLength = contentLength,
            contentType = contentType
        )

        scope.launch {
            if (downloadViewModel.isFileDownloaded(downloadRequest.fileName)) {
                // Show re-download confirmation (not implemented yet)
                Log.d("GeckoDownloadDelegate", "File already downloaded: ${downloadRequest.fileName} - Re-download confirmation needed (TODO)")
                showDownloadConfirmation(downloadRequest) // For now, show download confirmation even for re-downloads
            } else {
                // Show initial download confirmation
                Log.d("GeckoDownloadDelegate", "Showing download confirmation for: ${downloadRequest.fileName}")
                showDownloadConfirmation(downloadRequest)
            }
        }
    }
}