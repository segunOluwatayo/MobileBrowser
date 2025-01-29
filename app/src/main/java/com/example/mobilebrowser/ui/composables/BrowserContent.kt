package com.example.mobilebrowser.ui.composables

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
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
    geckoSession: GeckoSession,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onShowBookmarks: () -> Unit,
    onShowTabs: () -> Unit,
    onAddBookmark: (String, String) -> Unit,
    isCurrentUrlBookmarked: Boolean,
    currentPageTitle: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    currentUrl: String,
    onCanGoBackChange: (Boolean) -> Unit,
    onCanGoForwardChange: (Boolean) -> Unit,
    tabCount: Int,
    onNewTab: () -> Unit,
    onCloseAllTabs: () -> Unit,
    modifier: Modifier = Modifier
) {
    var urlText by remember { mutableStateOf(currentUrl) }
    var isEditing by remember { mutableStateOf(false) }
    var showTabMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(currentUrl) {
        if (!isEditing) {
            urlText = currentUrl
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Navigation bar with URL field
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

            // Tab button with counter and dropdown menu
            Box {
                BadgedBox(
                    badge = {
                        Badge {
                            Text(
                                text = tabCount.toString(),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                ) {
                    IconButton(onClick = { showTabMenu = true }) {
                        Icon(Icons.Default.Tab, contentDescription = "Tabs")
                    }
                }

                DropdownMenu(
                    expanded = showTabMenu,
                    onDismissRequest = { showTabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("View Tabs") },
                        onClick = {
                            showTabMenu = false
                            onShowTabs()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Tab, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("New Tab") },
                        onClick = {
                            showTabMenu = false
                            onNewTab()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Close All Tabs") },
                        onClick = {
                            showTabMenu = false
                            onCloseAllTabs()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    )
                }
            }

            // CHANGED: Star button is shown if the URL is not blank (no more "&& !currentUrl.startsWith('about:')")
            if (currentUrl.isNotBlank()) {
                IconButton(
                    onClick = {
                        if (!isCurrentUrlBookmarked) {
                            onAddBookmark(currentUrl, currentPageTitle)
                        }
                    }
                ) {
                    Icon(
                        if (isCurrentUrlBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isCurrentUrlBookmarked) "Bookmarked" else "Add bookmark",
                        tint = if (isCurrentUrlBookmarked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Overflow menu
            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }

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
                }
            }
        }

        // Browser view
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
