package com.example.mobilebrowser.ui.composables

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mobilebrowser.data.entity.TabEntity
import com.example.mobilebrowser.data.util.ThumbnailUtil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabListItem(
    tab: TabEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    isDragging: Boolean,
    onTabClick: () -> Unit,
    onCloseTab: () -> Unit,
    onBookmarkTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State to hold the result of verifying the thumbnail file.
    var isThumbnailValid by remember { mutableStateOf(false) }

    // Launch a coroutine when the thumbnail path changes.
    LaunchedEffect(tab.thumbnail) {
        isThumbnailValid = if (!tab.thumbnail.isNullOrEmpty()) {
            ThumbnailUtil.verifyThumbnailFile(tab.thumbnail!!)
        } else {
            false
        }
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 1.dp,
            pressedElevation = 4.dp
        ),
        modifier = modifier
            .size(width = 160.dp, height = 200.dp)
            .padding(4.dp)
            .combinedClickable(
                onClick = onTabClick,
                onLongClick = null // Long press handled by parent
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Thumbnail area (takes most of the space)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                // If a thumbnail path exists and the file is valid, display it.
                if (!tab.thumbnail.isNullOrEmpty() && isThumbnailValid) {
                    AsyncImage(
                        model = "file://${tab.thumbnail}",
                        contentDescription = "Tab Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = {
                            Log.d("TabListItem", "Successfully loaded thumbnail for tab ${tab.id}")
                        },
                        onError = { state ->
                            Log.e("TabListItem", "Error loading thumbnail for tab ${tab.id}: ${state.result.throwable?.message}")
                        }
                    )
                } else {
                    // Placeholder when no valid thumbnail is available.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Web,
                            contentDescription = "Web Page",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                // Close button overlay in top-right corner
                IconButton(
                    onClick = onCloseTab,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                        .padding(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Tab",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Domain/title bar at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
            ) {
                // Extract the domain from the tab URL.
                val domain = remember(tab.url) {
                    try {
                        val uri = java.net.URI(tab.url)
                        uri.host?.removePrefix("www.")?.takeIf { it.isNotEmpty() } ?: "New Tab"
                    } catch (e: Exception) {
                        "New Tab"
                    }
                }

                Text(
                    text = domain,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Page title
                Text(
                    text = tab.title.ifEmpty { "New Tab" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Selection/active indicator overlay.
            if (isSelected || isSelectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.medium
                        )
                )
            }
        }
    }
}

@Composable
fun TabListItemNewTabCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .size(width = 160.dp, height = 200.dp)
            .padding(4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Tab",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "New Tab",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
