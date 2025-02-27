package com.example.mobilebrowser.ui.homepage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
//    val domain = remember(tab.url) {
//        try {
//            val uri = java.net.URI(tab.url)
//            uri.host?.removePrefix("www.")?.takeIf { it.isNotEmpty() } ?: "New Tab"
//        } catch (e: Exception) {
//            "New Tab"
//        }
//    }

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
        // Wider thumbnail
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 60.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (!tab.thumbnail.isNullOrEmpty() && ThumbnailUtil.verifyThumbnailFile(tab.thumbnail)) {
                AsyncImage(
                    model = "file://${tab.thumbnail}",
                    contentDescription = "Tab Thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Title and URL with proper dark mode text colors
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tab.title.ifEmpty { "New Tab" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,  // Ensures visibility in dark mode
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = tab.url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,  // Secondary text color for dark mode
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}