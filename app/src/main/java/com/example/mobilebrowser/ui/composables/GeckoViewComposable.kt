package com.example.mobilebrowser.ui.composables

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    onViewCreated: (View) -> Unit,
    onScrollStopped: (View) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // Handler to debounce scroll events
    val scrollHandler = remember { Handler(Looper.getMainLooper()) }
    // Debounce delay in milliseconds
    val scrollStopDelay = 300L

    var geckoViewRef = remember { mutableStateOf<View?>(null) }

    DisposableEffect(geckoSession) {
        val scrollDelegate = object : GeckoSession.ScrollDelegate {
            override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
                // Reset any previous scroll stop callback
                scrollHandler.removeCallbacksAndMessages(null)
                scrollHandler.postDelayed({
                    geckoViewRef.value?.let { view ->
                        Log.d("GeckoViewComponent", "Scroll stopped; invoking onScrollStopped callback")
                        onScrollStopped(view)
                    }
                }, scrollStopDelay)
            }
        }
        geckoSession.setScrollDelegate(scrollDelegate)
        onDispose {
            geckoSession.setScrollDelegate(null)
            scrollHandler.removeCallbacksAndMessages(null)
        }
    }

    // Set up navigation delegate for handling URL changes and navigation state.
    DisposableEffect(geckoSession) {
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
        onDispose { }
    }

    Log.d("GeckoViewComponent", "Creating AndroidView for GeckoView with URL: $url")
    AndroidView(
        factory = { ctx ->
            Log.d("GeckoViewComponent", "Factory: Creating GeckoView instance")
            GeckoView(ctx).apply {
                setSession(geckoSession)
                geckoViewRef.value = this
                onViewCreated(this)
            }
        },
        modifier = modifier,
        update = { /*view ->
            Log.d("GeckoViewComponent", "Update: Loading URL: $url")
//            geckoSession.loadUri(url)
        */}
    )
}
