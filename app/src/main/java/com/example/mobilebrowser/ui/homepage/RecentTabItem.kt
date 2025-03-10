package com.example.mobilebrowser.ui.homepage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mobilebrowser.data.entity.TabEntity
import com.example.mobilebrowser.data.util.ThumbnailUtil

@Composable
fun RecentTabItem(
    tab: TabEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State to hold the result of verifying the thumbnail file.
    var isThumbnailValid by remember { mutableStateOf(false) }

    // Launch a coroutine whenever the thumbnail path changes.
    LaunchedEffect(tab.thumbnail) {
        isThumbnailValid = if (!tab.thumbnail.isNullOrEmpty()) {
            ThumbnailUtil.verifyThumbnailFile(tab.thumbnail!!)
        } else {
            false
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                shape = MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Thumbnail area.
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 60.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (!tab.thumbnail.isNullOrEmpty() && isThumbnailValid) {
                AsyncImage(
                    model = "file://${tab.thumbnail}",
                    contentDescription = "Tab Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Title and URL.
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tab.title.ifEmpty { "New Tab" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = tab.url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
