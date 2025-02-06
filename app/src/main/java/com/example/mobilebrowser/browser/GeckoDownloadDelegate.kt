package com.example.mobilebrowser.browser

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.mobilebrowser.ui.viewmodels.DownloadViewModel
import com.example.mobilebrowser.ui.util.PermissionHandler
import com.example.mobilebrowser.util.FileUtils
import com.example.mobilebrowser.util.FileUtils.fetchContentLength
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

        val headers = response.headers

        // Try to extract the filename from Content-Disposition header first.
        val contentDisposition = headers["Content-Disposition"]
        val rawFileName = if (contentDisposition != null) {
            FileUtils.extractFileNameFromContentDisposition(contentDisposition)
        } else {
            null
        } ?: Uri.parse(response.uri).lastPathSegment ?: "unknown"

        val safeFileName = FileUtils.getSafeFileName(rawFileName)

        // Case-insensitive lookup for content-length header
        val contentLengthHeader = headers.entries.firstOrNull {
            it.key.equals(
                "Content-Length",
                ignoreCase = true
            )
        }?.value
        var contentLength = contentLengthHeader?.toLongOrNull() ?: 0L

        // If contentLength is 0, perform a HEAD request to determine the file size.
        if (contentLength == 0L) {
            scope.launch {
                val headContentLength = fetchContentLength(response.uri)
                Log.d("GeckoDownloadDelegate", "HEAD request content length: $headContentLength")
                // Proceed with the rest of your logic using the value from the HEAD request
                continueDownload(
                    safeFileName,
                    response.uri,
                    headContentLength,
                    headers["Content-Type"] ?: "application/octet-stream"
                )
            }
        } else {
            scope.launch {
                continueDownload(
                    safeFileName,
                    response.uri,
                    contentLength,
                    headers["Content-Type"] ?: "application/octet-stream"
                )
            }
        }
    }

    // Extracted common function to continue with download processing.
    private suspend fun continueDownload(
        fileName: String,
        url: String,
        contentLength: Long,
        contentType: String
    ) {
        val downloadRequest = GeckoDownloadDelegate.DownloadRequest(
            fileName = fileName,
            url = url,
            contentLength = contentLength,
            contentType = contentType
        )

        if (downloadViewModel.isFileDownloaded(downloadRequest.fileName)) {
            Log.d(
                "GeckoDownloadDelegate",
                "File already downloaded: ${downloadRequest.fileName} - Re-download confirmation needed (TODO)"
            )
            showDownloadConfirmation(downloadRequest)
        } else {
            Log.d(
                "GeckoDownloadDelegate",
                "Showing download confirmation for: ${downloadRequest.fileName}"
            )
            showDownloadConfirmation(downloadRequest)
        }
    }
}