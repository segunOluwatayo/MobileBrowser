package com.example.mobilebrowser.ui.composables

import androidx.compose.animation.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.ui.viewmodels.TabViewModel
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
    modifier: Modifier = Modifier,
    tabViewModel: TabViewModel = hiltViewModel()
) {
    var urlText by remember { mutableStateOf(currentUrl) }
    var isEditing by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    val softwareKeyboardController = LocalSoftwareKeyboardController.current
    val tabCount by tabViewModel.tabCount.collectAsState()

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

            // Tabs button with counter
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                IconButton(onClick = onShowTabs) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Tab, contentDescription = "Tabs")
                        Text(text = tabCount.toString())
                    }
                }
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

            // Bookmark/Star button
            if (currentUrl.isNotBlank() && !currentUrl.startsWith("about:")) {
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
                    // Add more menu items here as needed
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