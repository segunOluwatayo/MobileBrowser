package com.example.mobilebrowser.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import org.mozilla.geckoview.GeckoSession

@Composable
fun BrowserContent(
    geckoSession: GeckoSession,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    currentUrl: String,
    onCanGoBackChange: (Boolean) -> Unit,
    onCanGoForwardChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        var urlText by remember { mutableStateOf(currentUrl) }
        var isEditing by remember { mutableStateOf(false) }
        val softwareKeyboardController = LocalSoftwareKeyboardController.current

        // Update urlText when currentUrl changes and we're not editing
        LaunchedEffect(currentUrl) {
            if (!isEditing) {
                urlText = currentUrl
            }
        }

        // Navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick = onBack,
                enabled = canGoBack
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
            }

            // Forward button
            IconButton(
                onClick = onForward,
                enabled = canGoForward
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Go forward")
            }

            // Reload button
            IconButton(onClick = {
                onReload()
                softwareKeyboardController?.hide()
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload")
            }

            // URL bar
            OutlinedTextField(
                value = urlText,
                onValueChange = {
                    isEditing = true
                    urlText = it
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                singleLine = true,
                label = { Text("Enter URL") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        isEditing = false
                        onNavigate(urlText)
                        softwareKeyboardController?.hide()
                    }
                )
            )
        }

        // GeckoView component
        GeckoViewComponent(
            geckoSession = geckoSession,
            url = currentUrl,
            onUrlChange = { newUrl ->
                if (!isEditing) {
                    onNavigate(newUrl)
                }
            },
            onCanGoBackChange = onCanGoBackChange,
            onCanGoForwardChange = onCanGoForwardChange,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}


//package com.example.mobilebrowser.ui.composables
//
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.text.KeyboardActions
//import androidx.compose.foundation.text.KeyboardOptions
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.OutlinedTextField
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalSoftwareKeyboardController
//import androidx.compose.ui.text.input.ImeAction
//import androidx.compose.ui.unit.dp
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material.icons.automirrored.filled.ArrowForward
//import androidx.compose.material.icons.filled.Refresh
//import org.mozilla.geckoview.GeckoSession
//
//@Composable
//fun BrowserContent(
//    geckoSession: GeckoSession,
//    onNavigate: (String) -> Unit,
//    onBack: () -> Unit,
//    onForward: () -> Unit,
//    onReload: () -> Unit,
//    canGoBack: Boolean,
//    canGoForward: Boolean,
//    currentUrl: String,
//    onCanGoBackChange: (Boolean) -> Unit,
//    onCanGoForwardChange: (Boolean) -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Column(
//        modifier = modifier.fillMaxSize()
//    ) {
//        var urlText by remember { mutableStateOf(currentUrl) }
//        val keyboardController = LocalSoftwareKeyboardController.current
//
//        // Navigation bar with URL field
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(8.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            // Back button
//            IconButton(
//                onClick = onBack,
//                enabled = canGoBack
//            ) {
//                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
//            }
//
//            // Forward button
//            IconButton(
//                onClick = onForward,
//                enabled = canGoForward
//            ) {
//                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Go forward")
//            }
//
//            // Reload button
//            IconButton(onClick = onReload) {
//                Icon(Icons.Default.Refresh, contentDescription = "Reload")
//            }
//
//            // URL input field
//            OutlinedTextField(
//                value = urlText,
//                onValueChange = { urlText = it },
//                modifier = Modifier
//                    .weight(1f)
//                    .padding(horizontal = 8.dp),
//                singleLine = true,
//                label = { Text("Enter URL") },
//                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
//                keyboardActions = KeyboardActions(
//                    onGo = {
//                        onNavigate(urlText)
//                        keyboardController?.hide()
//                    }
//                )
//            )
//        }
//
//        // Browser view
//        GeckoViewComponent(
//            geckoSession = geckoSession,
//            url = currentUrl,
//            onUrlChange = { newUrl ->
//                urlText = newUrl
//                onNavigate(newUrl)
//            },
//            onCanGoBackChange = onCanGoBackChange,
//            onCanGoForwardChange = onCanGoForwardChange,
//            modifier = Modifier
//                .weight(1f)
//                .fillMaxWidth()
//        )
//    }
//}