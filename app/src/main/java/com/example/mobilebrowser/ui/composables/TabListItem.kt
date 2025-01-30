package com.example.mobilebrowser.ui.composables

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mobilebrowser.data.entity.TabEntity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.DpOffset
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Language

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.TabListItem(
    tab: TabEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isDragging: Boolean,
    onTabClick: () -> Unit,
    onCloseTab: () -> Unit,
    onStartDrag: () -> Unit,
    onBookmarkTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }

    Card(
        shape = MaterialTheme.shapes.extraSmall, // More subtle rounded corners like Chrome
        elevation = if (isDragging) CardDefaults.elevatedCardElevation(8.dp)
        else CardDefaults.cardElevation(defaultElevation = 1.dp), // Lighter elevation for normal state
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp) // Tighter padding
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        contextMenuPosition = offset
                        showContextMenu = true
                    }
                )
            }
            .combinedClickable(
                onClick = onTabClick,
                onLongClick = {
                    showContextMenu = true
                }
            )
            .then(modifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp), // Reduced internal padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favicon/Icon area
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp) // Smaller icon
                    .padding(end = 8.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Title and URL
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = tab.title.ifEmpty { "New Tab" },
                    style = MaterialTheme.typography.bodyMedium, // Smaller text
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tab.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Close button
            IconButton(
                onClick = onCloseTab,
                modifier = Modifier.size(32.dp) // Smaller close button
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close tab",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    // Context menu
    DropdownMenu(
        expanded = showContextMenu,
        onDismissRequest = { showContextMenu = false },
        offset = DpOffset(
            x = contextMenuPosition.x.dp,
            y = contextMenuPosition.y.dp
        )
    ) {
        DropdownMenuItem(
            text = { Text("New tab") },
            onClick = {
                // Handle new tab creation
                showContextMenu = false
            },
            leadingIcon = {
                Icon(Icons.Default.Add, "New tab")
            }
        )
        DropdownMenuItem(
            text = { Text("Close tab") },
            onClick = {
                onCloseTab()
                showContextMenu = false
            },
            leadingIcon = {
                Icon(Icons.Default.Close, "Close tab")
            }
        )
        DropdownMenuItem(
            text = { Text("Bookmark tab") },
            onClick = {
                onBookmarkTab()
                showContextMenu = false
            },
            leadingIcon = {
                Icon(Icons.Default.BookmarkAdd, "Bookmark tab")
            }
        )
    }
}
