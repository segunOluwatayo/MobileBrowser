package com.example.mobilebrowser.browser

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.compose.runtime.collectAsState
import com.example.mobilebrowser.data.util.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebResponse
import java.util.concurrent.ConcurrentHashMap

class GeckoSessionManager(private val context: Context) {
    private val dataStoreManager = DataStoreManager(context)
    private val sessions = ConcurrentHashMap<Long, GeckoSession>()
    private var currentSession: GeckoSession? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    // Initialize GeckoRuntime with dark mode support
    private val geckoRuntime: GeckoRuntime by lazy {
        // Determine the current theme mode
        val themeMode = runBlocking { dataStoreManager.themeModeFlow.first() }

        // Set dark mode based on theme setting
        val isDarkMode = when(themeMode) {
            "DARK" -> true
            "LIGHT" -> false
            else -> { // "SYSTEM" mode
                val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                uiMode == Configuration.UI_MODE_NIGHT_YES
            }
        }

        Log.d("GeckoSessionManager", "Initializing with dark mode: $isDarkMode")

        // Create runtime with appropriate settings
        val settings = GeckoRuntimeSettings.Builder()
            .preferredColorScheme(if (isDarkMode)
                GeckoRuntimeSettings.COLOR_SCHEME_DARK
            else
                GeckoRuntimeSettings.COLOR_SCHEME_LIGHT)
            .build()

        GeckoRuntime.create(context, settings)
    }

    init {
        // Monitor theme changes and update all sessions
        scope.launch {
            dataStoreManager.themeModeFlow.collect { themeMode ->
                updateRuntimeColorScheme(themeMode)
            }
        }
    }

    private fun updateRuntimeColorScheme(themeMode: String) {
        val isDarkMode = when(themeMode) {
            "DARK" -> true
            "LIGHT" -> false
            else -> { // "SYSTEM" mode
                val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                uiMode == Configuration.UI_MODE_NIGHT_YES
            }
        }

        Log.d("GeckoSessionManager", "Updating color scheme to: ${if (isDarkMode) "dark" else "light"}")

        // Update color scheme
        val colorScheme = if (isDarkMode) {
            GeckoRuntimeSettings.COLOR_SCHEME_DARK
        } else {
            GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
        }

        geckoRuntime.settings.preferredColorScheme = colorScheme

        // Force reload of all sessions to apply the new theme
        sessions.forEach { (_, session) ->
            try {
                // Only reload active sessions with content
                if (session.isOpen) {
                    session.reload()
                }
            } catch (e: Exception) {
                Log.e("GeckoSessionManager", "Error reloading session", e)
            }
        }
    }

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