import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.mobilebrowser.R
import com.example.mobilebrowser.data.entity.ShortcutEntity


data class Shortcut(
    val iconRes: Int,
    val label: String,
    val url: String,
    val isPinned: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShortcutTile(
    shortcut: ShortcutEntity,       // Use ShortcutEntity directly
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(100.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Pin icon at the top-right corner, if pinned.
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Main shortcut icon.
                Icon(
                    painter = painterResource(id = shortcut.iconRes),
                    contentDescription = shortcut.label,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Shortcut label.
                Text(
                    text = shortcut.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

