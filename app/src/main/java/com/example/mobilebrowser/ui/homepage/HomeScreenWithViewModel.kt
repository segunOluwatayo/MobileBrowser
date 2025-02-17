package com.example.mobilebrowser.ui.homepage

import HomeScreen
import Shortcut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.data.entity.ShortcutEntity
import com.example.mobilebrowser.ui.viewmodels.ShortcutViewModel

/**
 * HomeScreenWithViewModel connects the ShortcutViewModel to the HomeScreen UI.
 * It observes the shortcuts state and passes the necessary event handlers.
 */
@Composable
fun HomeScreenWithViewModel(
    viewModel: ShortcutViewModel = hiltViewModel()
) {
    // Collect the current list of ShortcutEntity items from the ViewModel as state.
    val shortcutEntities by viewModel.shortcuts.collectAsState()

    // Convert ShortcutEntity to the UI model (Shortcut).
    // You can adjust this mapping if the UI model differs.
    val shortcuts = shortcutEntities.map { entity ->
        Shortcut(
            iconRes = entity.iconRes,
            label = entity.label,
            url = entity.url,
            isPinned = entity.isPinned
        )
    }

    // Render the HomeScreen with the shortcuts data and event handlers.
    HomeScreen(
        shortcuts = shortcuts,
        onShortcutLongPressed = { shortcut ->
            // Find the corresponding ShortcutEntity (if needed) and call the ViewModel handler.
            // For simplicity, we assume one-to-one mapping here.
            // Alternatively, modify the ViewModel to handle UI Shortcut objects directly.
            viewModel.onShortcutLongPress(
                // Convert the UI model back to entity if necessary.
                ShortcutEntity(
                    label = shortcut.label,
                    iconRes = shortcut.iconRes,
                    url = shortcut.url,
                    isPinned = shortcut.isPinned
                )
            )
        }
    )
}
