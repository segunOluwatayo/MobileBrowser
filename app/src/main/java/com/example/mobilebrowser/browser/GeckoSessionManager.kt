package com.example.mobilebrowser.browser

import android.content.Context
import android.util.Log
import com.example.mobilebrowser.BrowserApplication
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
            // First, check if this is an OAuth callback URL
            if (url?.contains("oauth-callback") == true &&
                url.contains("accessToken") &&
                url.contains("refreshToken")) {

                Log.d("GeckoSessionManager", "Detected OAuth callback URL: $url")
                try {
                    // Parse URL parameters
                    val uri = android.net.Uri.parse(url)

                    // Log the complete query string for debugging
                    Log.d("GeckoSessionManager", "Query string: ${uri.query}")

                    // Extract auth parameters
                    val accessToken = uri.getQueryParameter("accessToken")
                    val refreshToken = uri.getQueryParameter("refreshToken")
                    val userId = uri.getQueryParameter("userId")
                    val displayName = uri.getQueryParameter("displayName")
                    val email = uri.getQueryParameter("email")

                    // Log each parameter for debugging
                    Log.d("GeckoSessionManager", "accessToken: ${accessToken?.take(15)}...")
                    Log.d("GeckoSessionManager", "refreshToken: ${refreshToken?.take(15)}...")
                    Log.d("GeckoSessionManager", "userId: $userId")
                    Log.d("GeckoSessionManager", "displayName: $displayName")
                    Log.d("GeckoSessionManager", "email: $email")

                    if (accessToken != null && refreshToken != null) {
                        // Get AuthService from application context
                        val authService = (context.applicationContext as? BrowserApplication)?.getAuthService()

                        // Process the authentication data
                        authService?.processAuthCallback(accessToken, refreshToken, userId, displayName, email)

                        // Show success message
                        android.widget.Toast.makeText(
                            context,
                            "Signed in successfully",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()

                        // Navigate to homepage - using about:blank or your custom home page
                        session.loadUri("about:blank")

                        // Still call the onUrlChange callback so UI is updated
                        onUrlChange("about:blank")

                        return
                    } else {
                        Log.e("GeckoSessionManager", "Missing required tokens in OAuth callback")
                        android.widget.Toast.makeText(
                            context,
                            "Sign in failed: Missing authentication data",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("GeckoSessionManager", "Error processing OAuth callback", e)
                    android.widget.Toast.makeText(
                        context,
                        "Sign in error: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // Original behavior for normal URL changes
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