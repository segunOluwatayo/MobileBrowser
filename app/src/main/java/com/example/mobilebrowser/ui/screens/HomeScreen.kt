package com.example.mobilebrowser.ui.screens

import Shortcut
import ShortcutTile
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
import com.example.mobilebrowser.data.entity.ShortcutEntity

/**
 * HomeScreen displays a grid of shortcuts.
 *
 * @param shortcuts The list of shortcuts to display.
 * @param onShortcutLongPressed Callback when a shortcut is long-pressed.
 * @param onShortcutClick Callback when a shortcut is clicked.
 * @param modifier Modifier for styling.
 */
@Composable
fun HomeScreen(
    shortcuts: List<ShortcutEntity>,
    onShortcutClick: (ShortcutEntity) -> Unit,
    onShortcutLongPressed: (ShortcutEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 24.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(shortcuts) { shortcut ->
                ShortcutTile(
                    shortcut = shortcut,
                    onClick = { onShortcutClick(shortcut) },
                    onLongPress = { onShortcutLongPressed(shortcut) }
                )
            }

        }
    }
}