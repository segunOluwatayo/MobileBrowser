import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mobilebrowser.R
import com.example.mobilebrowser.data.entity.ShortcutEntity
import com.example.mobilebrowser.data.entity.ShortcutType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest


data class Shortcut(
    val iconRes: Int,
    val label: String,
    val url: String,
    val isPinned: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortcutTile(
    shortcut: ShortcutEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Extract domain for favicon fallback
    val domain = remember(shortcut.url) {
        try {
            val uri = java.net.URI(shortcut.url)
            uri.host?.removePrefix("www.")
        } catch (e: Exception) {
            null
        }
    }
    // Choose container color based on pinned/dynamic status
    val containerColor = when {
        shortcut.isPinned -> MaterialTheme.colorScheme.primaryContainer
        shortcut.shortcutType == ShortcutType.DYNAMIC -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    // Dynamic indicator color
    val dynamicColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)

    Card(
        modifier = modifier
            .size(100.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            // Automatically picks a readable color for text/icons based on containerColor
            contentColor = contentColorFor(containerColor)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Show pin icon if pinned
            if (shortcut.isPinned) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pin),
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                )
            }

            // If it's a dynamic shortcut and not pinned, show a small indicator
            if (shortcut.shortcutType == ShortcutType.DYNAMIC && !shortcut.isPinned) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .size(8.dp)
                ) {
                    drawCircle(
                        color = dynamicColor,
                        radius = size.minDimension / 2
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Favicon with fallback to hardcoded icon
                if (!shortcut.favicon.isNullOrEmpty() || domain != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(
                                shortcut.favicon ?: "https://www.google.com/s2/favicons?domain=$domain&sz=64"
                            )
                            .crossfade(true)
                            .build(),
                        contentDescription = shortcut.label,
                        modifier = Modifier.size(36.dp),
                        error = painterResource(id = shortcut.iconRes), // Fallback to hardcoded icon
                        fallback = painterResource(id = shortcut.iconRes)
                    )
                } else {
                    // Use hardcoded icon if no favicon available
                    Icon(
                        painter = painterResource(id = shortcut.iconRes),
                        contentDescription = shortcut.label,
                        modifier = Modifier.size(36.dp),
                        tint = Color.Unspecified
                    )
                }
//                Icon(
//                    painter = painterResource(id = shortcut.iconRes),
//                    contentDescription = shortcut.label,
//                    modifier = Modifier.size(36.dp),
//                    tint = Color.Unspecified
//                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = shortcut.label,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


