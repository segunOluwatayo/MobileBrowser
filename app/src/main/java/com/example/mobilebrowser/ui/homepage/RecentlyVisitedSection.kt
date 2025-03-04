package com.example.mobilebrowser.ui.homepage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.mobilebrowser.data.entity.HistoryEntity
import java.net.URI

@Composable
fun RecentlyVisitedSection(
    history: List<HistoryEntity>,
    onHistoryClick: (HistoryEntity) -> Unit,
    onShowAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        // Header section with title and "Show all" button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recently visited",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Show all",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onShowAllClick() }
            )
        }

        // Horizontal scrollable row of history cards
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(history) { historyEntry ->
                HistoryCard(
                    historyEntry = historyEntry,
                    onClick = { onHistoryClick(historyEntry) }
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(
    historyEntry: HistoryEntity,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // Extract domain for favicon fallback
    val domain = remember(historyEntry.url) {
        try {
            val uri = URI(historyEntry.url)
            uri.host?.removePrefix("www.")?.takeIf { it.isNotEmpty() } ?: "?"
        } catch (e: Exception) {
            "?"
        }
    }

    // Generate color based on domain for favicon fallback
    val domainColor = remember(domain) {
        val hash = domain.hashCode()
        val r = ((hash and 0xFF0000) shr 16) / 255f
        val g = ((hash and 0x00FF00) shr 8) / 255f
        val b = (hash and 0x0000FF) / 255f
        Color(r, g, b)
    }

    // Card with fixed width to ensure approximately 3 items per view
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favicon/Icon container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(domainColor),
                contentAlignment = Alignment.Center
            ) {
                // Generate favicon URL if not present
                val faviconUrl = remember(historyEntry.url) {
                    historyEntry.favicon ?: "https://$domain/favicon.ico"
                }

                // Track image loading state
                var imageLoaded by remember { mutableStateOf(false) }

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(faviconUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    contentScale = ContentScale.Fit,
                    onSuccess = { imageLoaded = true },
                    onError = { imageLoaded = false }
                )

                // Show the letter only when image hasn't loaded
                if (!imageLoaded) {
                    Text(
                        text = domain.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }

            // Title and URL
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = historyEntry.title.takeIf { it.isNotBlank() } ?: domain,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = domain,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}