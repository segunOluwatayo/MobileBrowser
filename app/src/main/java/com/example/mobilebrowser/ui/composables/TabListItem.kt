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
    onNewTab: () -> Unit,
    onBookmarkTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }

    Card(
        shape = MaterialTheme.shapes.extraSmall,
        elevation = if (isDragging) CardDefaults.elevatedCardElevation(8.dp)
        else CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
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
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab Title & URL
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = tab.title.ifEmpty { "New Tab" },
                    style = MaterialTheme.typography.bodyMedium,
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

            // Three Vertical Dots Button (Replaces Close Button)
            Box {
                IconButton(onClick = { showContextMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }

                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Close Tab") },
                        onClick = {
                            onCloseTab()
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Close, "Close Tab")
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Bookmark Tab") },
                        onClick = {
                            onBookmarkTab()
                            showContextMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.BookmarkAdd, "Bookmark Tab")
                        }
                    )
                }
            }
        }
    }
}
