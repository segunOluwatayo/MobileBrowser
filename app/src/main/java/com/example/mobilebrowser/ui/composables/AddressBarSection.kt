package com.example.mobilebrowser.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp


@Composable
fun AddressBarSection(
    urlText: String,
    currentUrl: String,
    isEditing: Boolean,
    onUrlTextChange: (String) -> Unit,
    onSearch: (String, SearchEngine) -> Unit,
    onNavigate: (String) -> Unit,
    currentSearchEngine: SearchEngine,
    onStartEditing: () -> Unit,
    onEndEditing: () -> Unit,
    tabCount: Int,
    onShowTabs: () -> Unit,
    showOverflowMenu: Boolean,
    onShowOverflowMenu: () -> Unit,
    onDismissOverflowMenu: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onAddBookmark: () -> Unit,
    isCurrentUrlBookmarked: Boolean,
    onShowBookmarks: () -> Unit,
    onShowHistory: () -> Unit,
    onShowDownloads: () -> Unit,
    onShowSettings: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Give the entire address bar row a background color that automatically adjusts
    // (MaterialTheme.colorScheme.surface is typical for a top/bottom bar).
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchUrlBar(
            value = urlText,
            currentUrl = currentUrl,
            onValueChange = onUrlTextChange,
            onSearch = { query, engine ->
                onSearch(query, engine)
                softwareKeyboardController?.hide()
                focusManager.clearFocus()
            },
            onNavigate = { url ->
                onNavigate(url)
                softwareKeyboardController?.hide()
                focusManager.clearFocus()
            },
            isEditing = isEditing,
            currentSearchEngine = currentSearchEngine,
            onStartEditing = onStartEditing,
            onEndEditing = onEndEditing,
            modifier = Modifier
                .weight(1f, fill = !isEditing)
                .focusRequester(focusRequester)
        )

        if (!isEditing) {
            // Tab button with a small colored box
            IconButton(onClick = onShowTabs) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        )
                        .clip(MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            // Overflow menu
            Box {
                IconButton(onClick = onShowOverflowMenu) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = onDismissOverflowMenu
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = {
                                onDismissOverflowMenu()
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
                                onDismissOverflowMenu()
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
                        if (currentUrl.isNotBlank()) {
                            IconButton(onClick = onAddBookmark) {
                                Icon(
                                    if (isCurrentUrlBookmarked) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = if (isCurrentUrlBookmarked) "Bookmarked" else "Add bookmark",
                                    tint = if (isCurrentUrlBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                onDismissOverflowMenu()
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
                            onDismissOverflowMenu()
                            onShowBookmarks()
                        },
                        leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                    )

                    DropdownMenuItem(
                        text = { Text("Downloads") },
                        onClick = {
                            onDismissOverflowMenu()
                            onShowDownloads()
                        },
                        leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    DropdownMenuItem(
                        text = { Text("History") },
                        onClick = {
                            onDismissOverflowMenu()
                            onShowHistory()
                        },
                        leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            onDismissOverflowMenu()
                            onShowSettings()
                        },
                        leadingIcon = { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                    )
                }
            }
        }
    }
}
