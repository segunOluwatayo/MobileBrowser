package com.example.mobilebrowser.ui.screens

import Shortcut
import ShortcutTile
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.data.entity.BookmarkEntity
import com.example.mobilebrowser.data.entity.HistoryEntity
import com.example.mobilebrowser.data.entity.ShortcutEntity
import com.example.mobilebrowser.data.entity.ShortcutType
import com.example.mobilebrowser.data.entity.TabEntity
import com.example.mobilebrowser.ui.composables.TabListItem
import com.example.mobilebrowser.ui.homepage.BookmarkSection
import com.example.mobilebrowser.ui.homepage.BookmarkTile
import com.example.mobilebrowser.ui.homepage.RecentTabItem
import com.example.mobilebrowser.ui.homepage.RecentlyVisitedSection
import com.example.mobilebrowser.ui.viewmodels.BookmarkViewModel
import com.example.mobilebrowser.ui.viewmodels.ShortcutViewModel

/**
 * HomeScreen displays a grid of shortcuts.
 *
 * @param shortcuts The list of shortcuts to display.
 * @param onShortcutLongPressed Callback when a shortcut is long-pressed.
 * @param onShortcutClick Callback when a shortcut is clicked.
 * @param modifier Modifier for styling.
 */
@Composable
fun HomeScreen(
    shortcuts: List<ShortcutEntity>,
    onShortcutClick: (ShortcutEntity) -> Unit,
    onShortcutLongPressed: (ShortcutEntity) -> Unit,
    onShowAllTabs: () -> Unit,
    onRecentTabClick: (TabEntity) -> Unit,
    onRestoreDefaultShortcuts: () -> Unit,
    onShowBookmarks: () -> Unit,
    recentTab: TabEntity? = null,
    recentHistory: List<HistoryEntity>,
    onRecentHistoryClick: (HistoryEntity) -> Unit,
    onShowAllHistory: () -> Unit,
    showShortcuts: Boolean = true, // New parameter to control visibility of shortcuts
    bookmarkViewModel: BookmarkViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // Collect bookmarks from the view model
    val bookmarks by bookmarkViewModel.bookmarks.collectAsState(initial = emptyList())

    // Group shortcuts by type
    val pinnedShortcuts = shortcuts.filter { it.isPinned }
    val dynamicShortcuts = shortcuts.filter { !it.isPinned && it.shortcutType == ShortcutType.DYNAMIC }

    // Create a scroll state that will allow the column to be scrolled
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .padding(top = 72.dp)
            // Add verticalScroll modifier to make the column scrollable
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nimbus Browser",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        // Conditionally show shortcuts based on the showShortcuts parameter
        if (showShortcuts) {
            // Pinned Shortcuts Section - add header row with Title and Restore button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Pinned Shortcuts",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (pinnedShortcuts.isEmpty()) {
                    Button(
                        onClick = onRestoreDefaultShortcuts,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restore defaults",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore Defaults")
                    }
                }
            }

            if (pinnedShortcuts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No pinned shortcuts available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Use a regular Grid instead of LazyVerticalGrid to avoid nested scrolling issues
                BoxedGrid(
                    items = pinnedShortcuts,
                    columns = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) { shortcut ->
                    ShortcutTile(
                        shortcut = shortcut,
                        onClick = { onShortcutClick(shortcut) },
                        onLongPress = { onShortcutLongPressed(shortcut) }
                    )
                }
            }

            if (dynamicShortcuts.isNotEmpty()) {
                Text(
                    text = "Frequently Visited",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(start = 16.dp, bottom = 8.dp, top = 16.dp)
                        .align(Alignment.Start)
                )

                // Use a regular Grid for frequently visited as well
                BoxedGrid(
                    items = dynamicShortcuts,
                    columns = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) { shortcut ->
                    ShortcutTile(
                        shortcut = shortcut,
                        onClick = { onShortcutClick(shortcut) },
                        onLongPress = { onShortcutLongPressed(shortcut) }
                    )
                }
            }
        }

        // The rest of the sections remain unchanged
        if (recentTab != null) {
            Spacer(modifier = Modifier.height(26.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Resume browsing",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Show all",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onShowAllTabs() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            RecentTabItem(
                tab = recentTab,
                onClick = { onRecentTabClick(recentTab) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (bookmarks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            BookmarkSection(
                bookmarks = bookmarks,
                onBookmarkClick = { bookmark ->
                    // Handle the bookmark click (e.g., navigate to the bookmark's URL)
                },
                onSeeAllClick = { onShowBookmarks() }
            )
        }

        if (recentHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            RecentlyVisitedSection(
                history = recentHistory,
                onHistoryClick = onRecentHistoryClick,
                onShowAllClick = onShowAllHistory
            )
        }

        // Add some padding at the bottom to ensure all content is visible
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * A simple grid implementation that doesn't use LazyVerticalGrid to avoid nested scrolling issues
 */
@Composable
fun <T> BoxedGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    Column(modifier = modifier) {
        val rows = (items.size + columns - 1) / columns // Calculate number of rows (ceiling division)

        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                    ) {
                        if (index < items.size) {
                            content(items[index])
                        }
                    }
                }
            }
        }
    }
}