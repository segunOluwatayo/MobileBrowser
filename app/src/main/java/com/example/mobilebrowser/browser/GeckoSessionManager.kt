package com.example.mobilebrowser.browser

import android.content.Context
import android.util.Log
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebResponse
import java.util.concurrent.ConcurrentHashMap

class GeckoSessionManager(private val context: Context) {
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
        downloadDelegate: GeckoSession.ContentDelegate? = null
    ): GeckoSession {
        return sessions.getOrPut(tabId) {
            GeckoSession().apply {
                open(geckoRuntime)
                navigationDelegate = createNavigationDelegate(onUrlChange, onCanGoBack, onCanGoForward)
                // Use a combined content delegate
                contentDelegate = createCombinedContentDelegate(onTitleChange, downloadDelegate)
                loadUri(url)
            }
        }.apply {
            navigationDelegate = createNavigationDelegate(onUrlChange, onCanGoBack, onCanGoForward)
            // Always update, in case callbacks change.  Crucial!
            contentDelegate = createCombinedContentDelegate(onTitleChange, downloadDelegate)
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

    // Create a combined content delegate
    private fun createCombinedContentDelegate(
        onTitleChange: (String) -> Unit,
        downloadDelegate: GeckoSession.ContentDelegate?
    ) = object : GeckoSession.ContentDelegate {

        override fun onTitleChange(session: GeckoSession, title: String?) {
            Log.d("GeckoSessionManager", "onTitleChange: $title")
            onTitleChange(title ?: "")
        }

        // Delegate download-related methods to the provided downloadDelegate
        override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
            downloadDelegate?.onExternalResponse(session, response)
        }

    }
}