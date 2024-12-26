package com.example.mobilebrowser.ui.composables

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

@Composable
fun GeckoViewComponent(
    url: String,
    onUrlChange: (String) -> Unit,
    onCanGoBackChange: (Boolean) -> Unit,
    onCanGoForwardChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val geckoRuntime = remember { getGeckoRuntime(context) }
    val geckoSession = remember {
        GeckoSession().apply {
            open(geckoRuntime)
            navigationDelegate = createNavigationDelegate(
                onUrlChange = onUrlChange,
                onCanGoBackChange = onCanGoBackChange,
                onCanGoForwardChange = onCanGoForwardChange
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            geckoSession.close()
        }
    }

    AndroidView(
        factory = { context ->
            GeckoView(context).apply {
                setSession(geckoSession)
            }
        },
        modifier = modifier
    ) { geckoView ->
        // Load URL if it has changed
        geckoSession.loadUri(url)
    }
}

private fun getGeckoRuntime(context: Context): GeckoRuntime {
    return GeckoRuntime.getDefault(context)
}

private fun createNavigationDelegate(
    onUrlChange: (String) -> Unit,
    onCanGoBackChange: (Boolean) -> Unit,
    onCanGoForwardChange: (Boolean) -> Unit
) = object : GeckoSession.NavigationDelegate {
    override fun onLocationChange(
        session: GeckoSession,
        url: String?,
        perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>
    ) {
        url?.let { onUrlChange(it) }
    }

    override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
        onCanGoBackChange(canGoBack)
    }

    override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
        onCanGoForwardChange(canGoForward)
    }
}