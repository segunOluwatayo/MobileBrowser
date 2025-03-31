package com.example.mobilebrowser.browser

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.mobilebrowser.BrowserApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebResponse
import java.util.concurrent.ConcurrentHashMap

class GeckoSessionManager(private val context: Context) {
    private val TAG = "GeckoSessionManager"
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
            // Always update, in case callbacks change. Crucial!
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
            // Log all URLs for debugging
            Log.d(TAG, "URL changed to: $url")

            // First, check if this is an OAuth callback URL
            if (url?.contains("oauth-callback") == true &&
                url.contains("accessToken") &&
                url.contains("refreshToken")) {

                Log.d(TAG, "Detected OAuth callback URL: $url")
                try {
                    // Parse URL parameters
                    val uri = Uri.parse(url)

                    // Log the complete query string for debugging
                    Log.d(TAG, "Query string: ${uri.query}")

                    // Extract auth parameters
                    val accessToken = uri.getQueryParameter("accessToken")
                    val refreshToken = uri.getQueryParameter("refreshToken")
                    val userId = uri.getQueryParameter("userId")
                    val displayName = uri.getQueryParameter("displayName")
                    val email = uri.getQueryParameter("email")

                    // Log each parameter for debugging
                    Log.d(TAG, "accessToken: ${accessToken?.take(15)}...")
                    Log.d(TAG, "refreshToken: ${refreshToken?.take(15)}...")
                    Log.d(TAG, "userId: $userId")
                    Log.d(TAG, "displayName: $displayName")
                    Log.d(TAG, "email: $email")

                    if (accessToken != null && refreshToken != null) {
                        // Get AuthService from application context
                        val authService = (context.applicationContext as? BrowserApplication)?.getAuthService()

                        // Process the authentication data
                        authService?.processAuthCallback(accessToken, refreshToken, userId, displayName, email)

                        // Show success message
                        Toast.makeText(
                            context,
                            "Signed in successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Navigate to homepage - using about:blank or your custom home page
                        session.loadUri("about:blank")

                        // Still call the onUrlChange callback so UI is updated
                        onUrlChange("about:blank")

                        return
                    } else {
                        Log.e(TAG, "Missing required tokens in OAuth callback")
                        Toast.makeText(
                            context,
                            "Sign in failed: Missing authentication data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing OAuth callback", e)
                    Toast.makeText(
                        context,
                        "Sign in error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // Enhanced logout URL detection
            // This ensures we detect intentional logouts from the web dashboard
            // while avoiding false positives for regular browsing
            val isLogoutUrl = when {
                // Direct custom scheme
                url?.startsWith("nimbusbrowser://logout") == true -> true

                // Web callback with action=logout parameter
                url?.contains("/oauth-callback") == true && url.contains("action=logout") -> true

                // Dashboard explicit sync logout (our custom parameter)
                url?.contains("/dashboard") == true && url.contains("sync_logout=true") -> true

                // Standard post-logout page or redirect
                url?.contains("logout_success") == true -> true

                // Standard web logout endpoints - detecting common patterns
                url?.contains("/logout") == true || url?.contains("signout") == true -> true

                // Detect logout response from server
                url?.contains("logged_out=true") == true -> true

                // Dashboard page that explicitly indicates logout via query parameter
                url?.contains("/dashboard") == true && url.contains("logout=true") -> true

                // Session ended or expired indicators
                url?.contains("session_expired") == true || url?.contains("session_ended") == true -> true

                else -> false
            }

            if (isLogoutUrl) {
                Log.d(TAG, "🔑 Detected logout URL: $url")
                try {
                    // Get AuthService from application context
                    val authService = (context.applicationContext as? BrowserApplication)?.getAuthService()

                    // Launch a coroutine to call the suspend function signOut
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            Log.d(TAG, "🔑 Calling authService.signOut()")
                            authService?.signOut()

                            // Show toast indicating logout
                            Toast.makeText(
                                context,
                                "Signed out successfully",
                                Toast.LENGTH_LONG
                            ).show()

                            // Navigate to homepage
                            session.loadUri("about:blank")

                            // Update UI
                            onUrlChange("about:blank")

                            Log.d(TAG, "🔑 Logout process completed successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error during sign out process", e)
                            Toast.makeText(
                                context,
                                "Error signing out: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    return
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling logout URL", e)
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
            Log.d(TAG, "onTitleChange: $title")
            onTitleChange(title ?: "")
        }

        // Delegate download-related methods to the provided downloadDelegate
        override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
            downloadDelegate?.onExternalResponse(session, response)
        }
    }
}