package com.example.mobilebrowser.ui.composables

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    onShowHistory: () -> Unit,
    onAddBookmark: (String, String) -> Unit,
    isCurrentUrlBookmarked: Boolean,
    currentPageTitle: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    currentUrl: String,
    onCanGoBackChange: (Boolean) -> Unit,
    onCanGoForwardChange: (Boolean) -> Unit,
    onShowDownloads: () -> Unit,
    tabCount: Int,
    onNewTab: () -> Unit,
    onCloseAllTabs: () -> Unit,
    modifier: Modifier = Modifier,
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
        // Navigation bar with URL field and buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                IconButton(onClick = { showTabMenu = true }) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                            .clip(MaterialTheme.shapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabCount.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(4.dp)
                        )
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
                        leadingIcon = { Icon(Icons.Default.Tab, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("New Tab") },
                        onClick = {
                            showTabMenu = false
                            onNewTab()
                        },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Close All Tabs") },
                        onClick = {
                            showTabMenu = false
                            onCloseAllTabs()
                        },
                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) }
                    )
                }
            }

            // Bookmark star button
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
                        tint = if (isCurrentUrlBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Overflow menu for navigation controls
            Box {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = {
                                showOverflowMenu = false
                                onBack()
                            },
                            enabled = canGoBack
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (canGoBack) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                            )
                        }

                        IconButton(
                            onClick = {
                                showOverflowMenu = false
                                onForward()
                            },
                            enabled = canGoForward
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (canGoForward) LocalContentColor.current else LocalContentColor.current.copy(alpha = 0.38f)
                            )
                        }

                        IconButton(
                            onClick = {
                                showOverflowMenu = false
                                onReload()
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    DropdownMenuItem(
                        text = { Text("Bookmarks") },
                        onClick = {
                            showOverflowMenu = false
                            onShowBookmarks()
                        },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                    )

                    DropdownMenuItem(
                        text = { Text("Downloads") },
                        onClick = {
                            showOverflowMenu = false
                            onShowDownloads()
                        },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    DropdownMenuItem(
                        text = { Text("History") },
                        onClick = {
                            showOverflowMenu = false
                            onShowHistory()
                        },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                    )
                }
            }
        }

        // Browser view area: wrap the GeckoViewComponent with a key based on the geckoSession.
        key(geckoSession) {
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
}
