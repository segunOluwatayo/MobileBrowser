package com.example.mobilebrowser

import android.os.Bundle
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

        setContent {
            MobileBrowserTheme {
                var currentUrl by remember { mutableStateOf("https://www.mozilla.org") }
                var canGoBack by remember { mutableStateOf(false) }
                var canGoForward by remember { mutableStateOf(false) }

                BrowserContent(
                    onNavigate = { url ->
                        currentUrl = url
                    },
                    onBack = {
                        geckoSession.goBack()
                    },
                    onForward = {
                        geckoSession.goForward()
                    },
                    onReload = {
                        geckoSession.reload()
                    },
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    currentUrl = currentUrl
                )
            }
        }
    }
}