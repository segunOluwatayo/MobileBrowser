package com.example.mobilebrowser.browser

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebResponse
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeckoSessionManager @Inject constructor(
    private val context: Context,
    private val downloadManager: DownloadManager
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

    // Updated content delegate that now also handles download events.
    private fun createContentDelegate(
        onTitleChange: (String) -> Unit,
        onDownloadStart: (WebResponse) -> Unit
    ) = object : GeckoSession.ContentDelegate {
        override fun onTitleChange(session: GeckoSession, title: String?) {
            onTitleChange(title ?: "")
        }
        // This callback is now used to notify about external responses (downloads)
        override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
            onDownloadStart(response)
        }
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

    // Helper function to open downloaded files
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
