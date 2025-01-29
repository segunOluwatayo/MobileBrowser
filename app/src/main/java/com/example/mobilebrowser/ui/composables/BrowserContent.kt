package com.example.mobilebrowser.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.mozilla.geckoview.GeckoSession

@Composable
fun BrowserContent(
    geckoSession: GeckoSession, // GeckoSession instance for managing web content.
    onNavigate: (String) -> Unit, // Callback for navigating to a new URL.
    onBack: () -> Unit, // Callback for navigating back.
    onForward: () -> Unit, // Callback for navigating forward.
    onReload: () -> Unit, // Callback for reloading the current page.
    onShowBookmarks: () -> Unit, // Callback for displaying the bookmarks screen.
    onAddBookmark: (String, String) -> Unit, // Callback for adding a bookmark with a URL and title.
    isCurrentUrlBookmarked: Boolean, // Whether the current URL is bookmarked.
    currentPageTitle: String, // Title of the current web page.
    canGoBack: Boolean, // Whether the browser can navigate back.
    canGoForward: Boolean, // Whether the browser can navigate forward.
    currentUrl: String, // The current URL displayed in the browser.
    onCanGoBackChange: (Boolean) -> Unit, // Callback for updating the back navigation state.
    onCanGoForwardChange: (Boolean) -> Unit, // Callback for updating the forward navigation state.
    modifier: Modifier = Modifier // Modifier for customizing the layout.
) {
    // Mutable states for managing URL bar text, editing state, and overflow menu visibility.
    var urlText by remember { mutableStateOf(currentUrl) }
    var isEditing by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    val softwareKeyboardController = LocalSoftwareKeyboardController.current // Keyboard controller.

    // Update the URL bar text when the current URL changes (only if not editing).
    LaunchedEffect(currentUrl) {
        if (!isEditing) {
            urlText = currentUrl
        }
    }

    // Column layout to structure the UI.
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Navigation bar with back, forward, reload buttons, URL bar, and bookmark options.
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

            // URL bar for displaying and entering URLs
            OutlinedTextField(
                value = urlText,
                onValueChange = {
                    isEditing = true
                    urlText = it
                },
                modifier = Modifier
                    .weight(1f) // Take up available horizontal space
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

            // Bookmark/Star button for adding or indicating bookmarked pages.
            if (currentUrl.isNotBlank() && !currentUrl.startsWith("about:")) {
                IconButton(
                    onClick = {
                        if (!isCurrentUrlBookmarked) {
                            onAddBookmark(currentUrl, currentPageTitle) // Add bookmark if not bookmarked.
                        }
                    }
                ) {
                    Icon(
                        if (isCurrentUrlBookmarked) Icons.Default.Star else Icons.Default.Star,
                        contentDescription = if (isCurrentUrlBookmarked) "Bookmarked" else "Add bookmark",
                        tint = if (isCurrentUrlBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Overflow menu for additional options.
            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }

                // Dropdown menu for showing bookmarks and other options.
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Bookmarks") },
                        onClick = {
                            showOverflowMenu = false
                            onShowBookmarks()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Star, contentDescription = null)
                        }
                    )
                    // Add more menu items here as needed.
                }
            }
        }

        // GeckoView component for rendering the web content.
        GeckoViewComponent(
            geckoSession = geckoSession,
            url = currentUrl,
            onUrlChange = { newUrl ->
                if (!isEditing) {
                    onNavigate(newUrl) // Navigate to a new URL when it changes.
                }
            },
            onCanGoBackChange = onCanGoBackChange, // Update back navigation state.
            onCanGoForwardChange = onCanGoForwardChange, // Update forward navigation state.
            modifier = Modifier
                .weight(1f) // Take up remaining space below the navigation bar.
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