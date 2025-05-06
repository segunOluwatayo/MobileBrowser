package com.example.mobilebrowser.browser

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.mobilebrowser.BrowserApplication
import com.example.mobilebrowser.classifier.UrlCnnInterpreter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class GeckoSessionManager(private val context: Context) {
    private val TAG = "GeckoSessionManager"
    private val geckoRuntime: GeckoRuntime by lazy { GeckoRuntime.getDefault(context) }
    private val sessions = ConcurrentHashMap<Long, GeckoSession>()
    private var currentSession: GeckoSession? = null

    // URL classifier for security checks
    private val urlClassifier: UrlCnnInterpreter by lazy { UrlCnnInterpreter(context) }

    // Callback for when a malicious URL is detected
    private var onMaliciousUrlDetected: ((String, String, () -> Unit, () -> Unit) -> Unit)? = null

    // Add an auth callback for tab handling
    private var onAuthSuccess: (() -> Unit)? = null

    // Method to set the auth success callback
    fun setAuthSuccessCallback(callback: () -> Unit) {
        onAuthSuccess = callback
    }

    // Method to set the malicious URL callback
    fun setMaliciousUrlCallback(callback: (String, String, () -> Unit, () -> Unit) -> Unit) {
        onMaliciousUrlDetected = callback
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
        // Flag to prevent infinite redirect loops
        private val pendingSecurityChecks = ConcurrentHashMap<String, AtomicBoolean>()

        override fun onLoadRequest(
            session: GeckoSession,
            request: GeckoSession.NavigationDelegate.LoadRequest
        ): GeckoResult<AllowOrDeny> {
            val url = request.uri

            // Skip checks for known safe URLs
            if (url.isNullOrEmpty() ||
                url == "about:blank" ||
                url.startsWith("about:") ||
                url.startsWith("chrome:") ||
                url.startsWith("resource:") ||
                url.startsWith("file:") ||
                url.startsWith("data:") ||
                url.startsWith("blob:")) {
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            // Skip OAuth callback URLs and logout URLs
            if ((url.contains("nimbus-browser-backend-production.up.railway.app") &&
                        url.contains("oauth-callback")) || isLogoutUrl(url)) {
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            // Create a GeckoResult we can complete later
            val result = GeckoResult<AllowOrDeny>()

            // Check if we already verified this URL to avoid duplicates
            val pending = pendingSecurityChecks.getOrPut(url) { AtomicBoolean(false) }
            if (pending.getAndSet(true)) {
                // This URL is already being checked, allow navigation to avoid duplicates
                pendingSecurityChecks.remove(url)
                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
            }

            // Check URL safety asynchronously
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    Log.d(TAG, "Checking safety of URL: $url")
                    val verdict = urlClassifier.verdict(url)
                    Log.d(TAG, "Classifier verdict for $url: $verdict")

                    val isMalicious = verdict.startsWith("Malicious")

                    withContext(Dispatchers.Main) {
                        if (isMalicious) {
                            // Show warning dialog and wait for user decision
                            onMaliciousUrlDetected?.invoke(
                                url,
                                verdict,
                                // Proceed callback
                                {
                                    Log.d(TAG, "User chose to proceed to malicious URL: $url")
                                    result.complete(AllowOrDeny.ALLOW)
                                    pendingSecurityChecks.remove(url)
                                },
                                // Go back callback
                                {
                                    Log.d(TAG, "User chose to go back from malicious URL: $url")
                                    result.complete(AllowOrDeny.DENY)
                                    pendingSecurityChecks.remove(url)
                                }
                            )
                        } else {
                            // URL is safe, allow navigation
                            result.complete(AllowOrDeny.ALLOW)
                            pendingSecurityChecks.remove(url)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during URL safety check", e)
                    // On error, allow navigation to continue
                    withContext(Dispatchers.Main) {
                        result.complete(AllowOrDeny.ALLOW)
                        pendingSecurityChecks.remove(url)
                    }
                }
            }

            return result
        }

        // Helper function to check if a URL is a logout URL
        private fun isLogoutUrl(url: String): Boolean {
            return url.startsWith("nimbusbrowser://logout") ||
                    url.contains("/oauth-callback") && url.contains("action=logout") ||
                    url.contains("/dashboard") && url.contains("sync_logout=true") ||
                    url.contains("logout_success") ||
                    url.contains("nimbus-browser-backend-production.up.railway.app") &&
                    (url.contains("/logout") || url.contains("signout")) ||
                    url.contains("logged_out=true") ||
                    url.contains("/dashboard") && url.contains("logout=true") ||
                    url.contains("session_expired") || url.contains("session_ended")
        }

        // Keep your existing onLocationChange method, but simplify it since
        // the security check now happens in onLoadRequest
        override fun onLocationChange(
            session: GeckoSession,
            url: String?,
            perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>
        ) {
            // Log all URLs for debugging
            Log.d(TAG, "URL changed to: $url")

            // Skip null or empty URLs
            if (url.isNullOrEmpty() || url == "about:blank") {
                onUrlChange("")
                return
            }

            // Process OAuth callback URLs
            if (url.contains("nimbus-browser-backend-production.up.railway.app") &&
                url.contains("oauth-callback") &&
                url.contains("accessToken")) {
                // Your existing OAuth callback code here...
                return
            }

            // Process logout URLs
            if (isLogoutUrl(url)) {
                // Your existing logout handling code here...
                return
            }

            // For all other URLs, we've already checked safety in onLoadRequest
            onUrlChange(url)
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