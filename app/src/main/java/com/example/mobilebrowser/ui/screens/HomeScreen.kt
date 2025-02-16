import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    shortcuts: List<Shortcut>,
    onShortcutLongPressed: (Shortcut) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = 72.dp), // Add padding for the URL bar
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Welcome message or browser logo could go here
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        // Grid of shortcuts
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(shortcuts) { shortcut ->
                ShortcutTile(
                    iconResId = shortcut.iconRes,
                    label = shortcut.label,
                    pinned = shortcut.isPinned,
                    onClick = { /* Handle shortcut click */ },
                    onLongPress = { onShortcutLongPressed(shortcut) }
                )
            }
        }
    }
}