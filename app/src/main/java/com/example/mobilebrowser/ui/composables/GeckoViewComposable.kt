
package com.example.mobilebrowser.ui.composables

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

@Composable
fun GeckoViewComponent(
    geckoSession: GeckoSession,
    url: String,
    onUrlChange: (String) -> Unit,
    onCanGoBackChange: (Boolean) -> Unit,
    onCanGoForwardChange: (Boolean) -> Unit,
    onViewCreated: (android.view.View) -> Unit,
    modifier: Modifier = Modifier
) {
    LocalContext.current

    // Configure navigation delegate for the shared session
    geckoSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
        override fun onLocationChange(
            session: GeckoSession,
            url: String?,
            perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>
        ) {
            Log.d("GeckoViewComponent", "onLocationChange: $url")
            url?.let { onUrlChange(it) }
        }

        override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
            Log.d("GeckoViewComponent", "onCanGoBack: $canGoBack")
            onCanGoBackChange(canGoBack)
        }

        override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
            Log.d("GeckoViewComponent", "onCanGoForward: $canGoForward")
            onCanGoForwardChange(canGoForward)
        }
    }

    Log.d("GeckoViewComponent", "Creating AndroidView for GeckoView with URL: $url")
    AndroidView(
        factory = { context ->
            Log.d("GeckoViewComponent", "Factory: Creating GeckoView instance")
            GeckoView(context).apply {
                setSession(geckoSession)
                onViewCreated(this)
            }
        },
        modifier = modifier,
        update = { view ->
            Log.d("GeckoViewComponent", "Update: Loading URL: $url")
            geckoSession.loadUri(url)
        }
    )
}

