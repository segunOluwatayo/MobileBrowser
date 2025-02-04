
package com.example.mobilebrowser.browser

import android.app.DownloadManager
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
                contentDelegate = createContentDelegate(onTitleChange, onDownloadStart)
                loadUri(url)
            }
        }.apply {
            navigationDelegate = createNavigationDelegate(onUrlChange, onCanGoBack, onCanGoForward)
            contentDelegate = createContentDelegate(onTitleChange, onDownloadStart)
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
        onDownloadStart: (WebResponse) -> Unit
    ) = object : GeckoSession.ContentDelegate {
        override fun onTitleChange(session: GeckoSession, title: String?) {
            onTitleChange(title ?: "")
        }

        override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
            // Check if the response headers indicate an attachment download.
            val contentDisposition = response.headers["Content-Disposition"] ?: ""
            Log.d("GeckoSessionManager", "Content-Disposition: $contentDisposition")
            if (contentDisposition.contains("attachment", ignoreCase = true)) {
                val uri = Uri.parse(response.uri.toString())
                val filename = extractFilename(response.headers["Content-Disposition"])
                    ?: (uri.lastPathSegment ?: "download")
                val mimeType = response.headers["Content-Type"] ?: "application/octet-stream"

                val request = DownloadManager.Request(uri)
                    .setMimeType(mimeType)
                    .setTitle(filename)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)

                // Enqueue the download and capture the download ID.
                val downloadId = downloadManager.enqueue(request)
                Log.d("GeckoSessionManager", "Download enqueued with id: $downloadId")

                // Create a new download record in your local database.
                CoroutineScope(Dispatchers.IO).launch {
                    // Note: fileSize is unknown at this stage; using 0 as a placeholder.
                    downloadRepository.createDownload(
                        filename = filename,
                        mimeType = mimeType,
                        fileSize = 0L,
                        sourceUrl = response.uri.toString(),
                        contentDisposition = response.headers["Content-Disposition"]
                    )
                }
            } else {
                Log.d("GeckoSessionManager", "onDownloadStart callback triggered")
                onDownloadStart(response)
            }
        }
    }

    // Helper function to extract filename from a Content-Disposition header.
    private fun extractFilename(contentDisposition: String?): String? {
        if (contentDisposition == null) return null
        // Look for a pattern like: filename="example.mp3"
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
