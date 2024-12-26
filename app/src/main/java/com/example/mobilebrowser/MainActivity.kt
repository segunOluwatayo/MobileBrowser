package com.example.mobilebrowser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.mobilebrowser.ui.composables.BrowserContent
import com.example.mobilebrowser.ui.theme.MobileBrowserTheme  // Updated import

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobileBrowserTheme {  // Updated theme name
                val currentUrl by remember { mutableStateOf("https://www.mozilla.org") }
                val canGoBack by remember { mutableStateOf(false) }
                val canGoForward by remember { mutableStateOf(false) }

                BrowserContent(
                    onNavigate = { url -> /* Will implement in next step */ },
                    onBack = { /* Will implement in next step */ },
                    onForward = { /* Will implement in next step */ },
                    onReload = { /* Will implement in next step */ },
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    currentUrl = currentUrl
                )
            }
        }
    }
}