package com.example.mobilebrowser.ui.homepage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.mobilebrowser.R
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
        // Header row with "Recently visited" and "Show all"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recently visited",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "Show all",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onShowAllClick() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History items with dividers
        history.take(5).forEachIndexed { index, historyEntry ->
            HistoryItem(
                historyEntry = historyEntry,
                onClick = { onHistoryClick(historyEntry) },
                modifier = Modifier.fillMaxWidth()
            )

            // Add divider except after the last item
            if (index < history.size - 1 && index < 4) {
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            }
        }
    }
}

@Composable
private fun HistoryItem(
    historyEntry: HistoryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Favicon with fallback
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
                modifier = Modifier.fillMaxSize().padding(8.dp),
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

        Spacer(modifier = Modifier.width(16.dp))

        // Title or URL
        Text(
            text = historyEntry.title.takeIf { it.isNotBlank() } ?: domain,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}