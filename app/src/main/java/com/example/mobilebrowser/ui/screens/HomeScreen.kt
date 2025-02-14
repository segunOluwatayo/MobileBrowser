import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mobilebrowser.R

@Composable
fun HomeScreen(
    shortcuts: List<Shortcut>,
    onShortcutLongPressed: (Shortcut) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(shortcuts) { shortcut ->
            ShortcutTile(
                iconResId = shortcut.iconRes,
                label = shortcut.label,
                pinned = shortcut.isPinned,
                onClick = {
                    // For now, no action on simple tap. You could log or handle this if needed.
                },
                onLongPress = { onShortcutLongPressed(shortcut) }
            )
        }
    }

    @Composable
    fun HomeScreenPreview() {
        // Sample shortcuts
        val sampleShortcuts = listOf(
            Shortcut(iconRes = R.drawable.google_icon, label = "Google", url = "https://www.google.com", isPinned = true),
            Shortcut(iconRes = R.drawable.bing_icon, label = "Bing", url = "https://www.bing.com"),
            Shortcut(iconRes = R.drawable.duckduckgo_icon, label = "DuckDuckGo", url = "https://www.duckduckgo.com")
        )

        HomeScreen(
            shortcuts = sampleShortcuts,
//            onShortcutSelected = { url -> /* Handle shortcut tap, e.g., open URL in new tab */ },
            onShortcutLongPressed = { shortcut -> /* Handle long press, e.g., show options to edit or unpin */ }
        )
    }
}
