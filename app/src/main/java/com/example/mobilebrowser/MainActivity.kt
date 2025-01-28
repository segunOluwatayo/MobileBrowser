package com.example.mobilebrowser

import android.os.Bundle
import android.util.Log // Import Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.mobilebrowser.ui.composables.BrowserContent
import com.example.mobilebrowser.ui.theme.MobileBrowserTheme
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

class MainActivity : ComponentActivity() {
    private lateinit var geckoRuntime: GeckoRuntime
    private lateinit var geckoSession: GeckoSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize GeckoRuntime
        geckoRuntime = GeckoRuntime.getDefault(this)

        // Initialize and open GeckoSession
        geckoSession = GeckoSession()
        geckoSession.open(geckoRuntime)
        Log.d("MainActivity", "GeckoSession opened in onCreate")

        setContent {
            MobileBrowserTheme {
                var currentUrl by remember { mutableStateOf("https://www.mozilla.org") }
                var canGoBack by remember { mutableStateOf(false) }
                var canGoForward by remember { mutableStateOf(false) }

                BrowserContent(
                    geckoSession = geckoSession,  // Pass the GeckoSession instance
                    onNavigate = { url ->
                        currentUrl = url
                    },
                    onBack = {
                        Log.d("MainActivity", "Go Back Button Clicked")
                        geckoSession.goBack()
                    },
                    onForward = {
                        Log.d("MainActivity", "Go Forward Button Clicked")
                        geckoSession.goForward()
                    },
                    onReload = {
                        Log.d("MainActivity", "Reload Button Clicked")
                        geckoSession.reload(GeckoSession.LOAD_FLAGS_BYPASS_CACHE)
                    },
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    currentUrl = currentUrl,
                    onCanGoBackChange = { isBack ->
                        canGoBack = isBack
                        Log.d("MainActivity", "onCanGoBackChange: $isBack")
                    },
                    onCanGoForwardChange = { isForward ->
                        canGoForward = isForward
                        Log.d("MainActivity", "onCanGoForwardChange: $isForward")
                    }
                )
            }
        }
    }
}