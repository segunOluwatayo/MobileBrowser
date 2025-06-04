package com.example.mobilebrowser.ui.homepage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mobilebrowser.data.entity.BookmarkEntity
import java.io.File

@Composable
fun BookmarkTile(
    bookmark: BookmarkEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .width(180.dp)
            .height(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Thumbnail area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                // Check if bookmark has a thumbnail
                if (bookmark.favicon?.startsWith("file://") == true) {
                    // Load local thumbnail file
                    val thumbnailFile = File(bookmark.favicon!!.removePrefix("file://"))
                    if (thumbnailFile.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(thumbnailFile)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Bookmark Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback when thumbnail file doesn't exist
                        FallbackThumbnail(bookmark.url)
                    }
                } else if (!bookmark.favicon.isNullOrEmpty()) {
                    // Load favicon from URL
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(bookmark.favicon)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Bookmark Icon",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback thumbnail
                    FallbackThumbnail(bookmark.url)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .align(Alignment.TopStart)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                }
            }

            // Title bar at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
            ) {
                Text(
                    text = bookmark.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun FallbackThumbnail(url: String) {
    // Generate a background color based on URL
    val domain = try {
        java.net.URI(url).host?.removePrefix("www.")?.take(2) ?: "un"
    } catch (e: Exception) {
        "un"
    }

    // Generate a simple color based on the domain name
    val hash = domain.hashCode()
    val r = ((hash and 0xFF0000) shr 16) / 255f
    val g = ((hash and 0x00FF00) shr 8) / 255f
    val b = (hash and 0x0000FF) / 255f
    val color = Color(r, g, b).copy(alpha = 0.7f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Web,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Display domain name
            Text(
                text = domain.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}