package com.example.mobilebrowser.ui.screens

import Shortcut
import ShortcutTile
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.data.entity.ShortcutEntity
import com.example.mobilebrowser.data.entity.ShortcutType
import com.example.mobilebrowser.data.entity.TabEntity
import com.example.mobilebrowser.ui.composables.TabListItem
import com.example.mobilebrowser.ui.homepage.RecentTabItem
import com.example.mobilebrowser.ui.viewmodels.ShortcutViewModel

/**
 * HomeScreen displays a grid of shortcuts.
 *
 * @param shortcuts The list of shortcuts to display.
 * @param onShortcutLongPressed Callback when a shortcut is long-pressed.
 * @param onShortcutClick Callback when a shortcut is clicked.
 * @param modifier Modifier for styling.
 */
// In HomeScreen.kt, update the function signature
@Composable
fun HomeScreen(
    shortcuts: List<ShortcutEntity>,
    onShortcutClick: (ShortcutEntity) -> Unit,
    onShortcutLongPressed: (ShortcutEntity) -> Unit,
    onShowAllTabs: () -> Unit,
    onRecentTabClick: (TabEntity) -> Unit,
    onRestoreDefaultShortcuts: () -> Unit, // Add this parameter
    recentTab: TabEntity? = null,
    modifier: Modifier = Modifier
) {
    // Group shortcuts by type
    val pinnedShortcuts = shortcuts.filter { it.isPinned }
    val dynamicShortcuts = shortcuts.filter { !it.isPinned && it.shortcutType == ShortcutType.DYNAMIC }

    Column(
        modifier = modifier.padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(vertical = 24.dp)
        )

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
                color = MaterialTheme.colorScheme.secondary,
            )

            // Only show restore button if no pinned shortcuts exist
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

        // Empty state message when no shortcuts
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
            // The existing LazyVerticalGrid for pinned shortcuts
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(pinnedShortcuts) { shortcut ->
                    ShortcutTile(
                        shortcut = shortcut,
                        onClick = { onShortcutClick(shortcut) },
                        onLongPress = { onShortcutLongPressed(shortcut) }
                    )
                }
            }
        }

        // Dynamic Shortcuts Section
        if (dynamicShortcuts.isNotEmpty()) {
            Text(
                text = "Frequently Visited",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 8.dp, top = 16.dp)
                    .align(Alignment.Start)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(dynamicShortcuts) { shortcut ->
                    ShortcutTile(
                        shortcut = shortcut,
                        onClick = { onShortcutClick(shortcut) },
                        onLongPress = { onShortcutLongPressed(shortcut) }
                    )
                }
            }
        }

        // Resume browsing section - only if recentTab is not a new tab
        if (recentTab != null) {
            Spacer(modifier = Modifier.height(16.dp))
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
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Show all",
                    style = MaterialTheme.typography.bodyMedium,
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
    }
}
