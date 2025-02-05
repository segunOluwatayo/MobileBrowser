package com.example.mobilebrowser.browser

import android.app.DownloadManager
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.example.mobilebrowser.data.repository.DownloadRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebResponse
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeckoSessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: DownloadManager,
    private val downloadRepository: DownloadRepository
) {
    private val geckoRuntime: GeckoRuntime by lazy { GeckoRuntime.getDefault(context) }
    private val sessions = ConcurrentHashMap<Long, GeckoSession>()
    private var currentSession: GeckoSession? = null

    // New callback for download confirmation
    var onDownloadRequested: (
        filename: String,
        mimeType: String,
        sourceUrl: String,
        contentDisposition: String?
    ) -> Unit = { _, _, _, _ -> }

    // Function to initiate download after confirmation
    fun initiateDownload(
        filename: String,
        mimeType: String,
        sourceUrl: String,
        contentDisposition: String?
    ) {
        Log.d(TAG, "Starting download for $filename")
        try {
            val uri = Uri.parse(sourceUrl)
            val request = DownloadManager.Request(uri).apply {
                setMimeType(mimeType)
                setTitle(filename)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            }

            // Get the actual Android DownloadManager ID
            val androidDownloadId = downloadManager.enqueue(request)
            Log.d(TAG, "Android DownloadManager ID: $androidDownloadId")

            // Create the download entry with the Android DownloadManager ID
            CoroutineScope(Dispatchers.IO).launch {
                downloadRepository.createDownload(
                    filename = filename,
                    mimeType = mimeType,
                    fileSize = 0L, // We'll update this when download completes
                    sourceUrl = sourceUrl,
                    contentDisposition = contentDisposition,
                    androidDownloadId = androidDownloadId
                )
                Log.d(TAG, "Download entry created in repository")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating download", e)
        }
    }

    fun getOrCreateSession(
        tabId: Long,
        url: String = "about:blank",
        onUrlChange: (String) -> Unit = {},
        onTitleChange: (String) -> Unit = {},
        onCanGoBack: (Boolean) -> Unit = {},
        onCanGoForward: (Boolean) -> Unit = {},
        onDownloadStart: (WebResponse) -> Unit = {}
    ): GeckoSession {
        return sessions.getOrPut(tabId) {
            GeckoSession().apply {
                open(geckoRuntime)
                navigationDelegate = createNavigationDelegate(onUrlChange, onCanGoBack, onCanGoForward)
                contentDelegate = createContentDelegate(onTitleChange)
                loadUri(url)
            }
        }.apply {
            navigationDelegate = createNavigationDelegate(onUrlChange, onCanGoBack, onCanGoForward)
            contentDelegate = createContentDelegate(onTitleChange)
        }
    }

    private fun createNavigationDelegate(
        onUrlChange: (String) -> Unit,
        onCanGoBack: (Boolean) -> Unit,
        onCanGoForward: (Boolean) -> Unit
    ) = object : GeckoSession.NavigationDelegate {
        override fun onLocationChange(
            session: GeckoSession,
            url: String?,
            perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>
        ) {
            url?.let { onUrlChange(it) }
        }

        override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
            onCanGoBack(canGoBack)
        }

        override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
            onCanGoForward(canGoForward)
        }
    }

    private fun createContentDelegate(
        onTitleChange: (String) -> Unit,
    ) = object : GeckoSession.ContentDelegate {
        override fun onTitleChange(session: GeckoSession, title: String?) {
            onTitleChange(title ?: "")
        }

        override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
            val contentDisposition = response.headers["Content-Disposition"] ?: ""
            if (contentDisposition.contains("attachment", ignoreCase = true)) {
                val filename = extractFilename(response.headers["Content-Disposition"])
                    ?: (Uri.parse(response.uri.toString()).lastPathSegment ?: "download")
                val mimeType = response.headers["Content-Type"] ?: "application/octet-stream"
                val sourceUrl = response.uri.toString()
                val cdHeader = response.headers["Content-Disposition"]

                // Call the confirmation callback instead of starting download immediately
                onDownloadRequested(filename, mimeType, sourceUrl, cdHeader)
            }
        }
    }

    private fun extractFilename(contentDisposition: String?): String? {
        if (contentDisposition == null) return null
        val regex = "filename=\"(.*?)\"".toRegex()
        val matchResult = regex.find(contentDisposition)
        return matchResult?.groups?.get(1)?.value
    }

    fun switchToSession(tabId: Long): GeckoSession? {
        return sessions[tabId]?.also { currentSession = it }
    }

    fun getCurrentSession(): GeckoSession? = currentSession

    fun removeSession(tabId: Long) {
        sessions[tabId]?.close()
        sessions.remove(tabId)
        if (sessions.isEmpty()) {
            currentSession = null
        }
    }

    fun removeAllSessions() {
        sessions.forEach { (_, session) -> session.close() }
        sessions.clear()
        currentSession = null
    }

    // Helper function to open downloaded files.
    fun openDownloadedFile(file: File, mimeType: String): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
