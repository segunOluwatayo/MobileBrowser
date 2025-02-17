package com.example.mobilebrowser.screens

import Shortcut
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.data.entity.ShortcutEntity
import com.example.mobilebrowser.ui.screens.HomeScreen
import com.example.mobilebrowser.ui.homepage.ShortcutOptionsDialog
import com.example.mobilebrowser.ui.viewmodels.ShortcutViewModel

/**
 * HomeScreenWithDialog integrates HomeScreen with a context dialog for shortcut actions.
 * It observes data from ShortcutViewModel and displays the ShortcutOptionsDialog when needed.
 */
@Composable
fun HomeScreenWithDialog(viewModel: ShortcutViewModel = hiltViewModel()) {
    // Observe the list of shortcuts from the ViewModel.
    val shortcutEntities by viewModel.shortcuts.collectAsState()

    // Map ShortcutEntity objects to UI model (Shortcut).
    val shortcuts = shortcutEntities.map { entity ->
        Shortcut(
            iconRes = entity.iconRes,
            label = entity.label,
            url = entity.url,
            isPinned = entity.isPinned
        )
    }

    // State variable to track which shortcut is selected (for long press).
    var selectedShortcut by remember { mutableStateOf<Shortcut?>(null) }

    // Render the HomeScreen.
    HomeScreen(
        shortcuts = shortcuts,
        onShortcutClick = { shortcut ->
            // Handle normal click (for example, open the URL in the current tab).
            viewModel.onShortcutClick(
                ShortcutEntity(
                    label = shortcut.label,
                    iconRes = shortcut.iconRes,
                    url = shortcut.url,
                    isPinned = shortcut.isPinned
                )
            )
        },
        onShortcutLongPressed = { shortcut ->
            // Set the selected shortcut to show the options dialog.
            selectedShortcut = shortcut
        }
    )

    // If a shortcut is selected, display the options dialog.
    selectedShortcut?.let { shortcut ->
        ShortcutOptionsDialog(
            shortcut = shortcut,
            onDismiss = { selectedShortcut = null },
            onOpenInNewTab = {
                viewModel.onShortcutClick(
                    ShortcutEntity(
                        label = shortcut.label,
                        iconRes = shortcut.iconRes,
                        url = shortcut.url,
                        isPinned = shortcut.isPinned
                    )
                )
                selectedShortcut = null
            },
            onEdit = {
                // TODO: Implement edit functionality.
                selectedShortcut = null
            },
            onTogglePin = {
                // Update the shortcut's pinned status.
                val updatedShortcut = ShortcutEntity(
                    label = shortcut.label,
                    iconRes = shortcut.iconRes,
                    url = shortcut.url,
                    isPinned = !shortcut.isPinned
                )
                viewModel.updateShortcut(updatedShortcut)
                selectedShortcut = null
            },
            onDelete = {
                val entityToDelete = ShortcutEntity(
                    label = shortcut.label,
                    iconRes = shortcut.iconRes,
                    url = shortcut.url,
                    isPinned = shortcut.isPinned
                )
                viewModel.deleteShortcut(entityToDelete)
                selectedShortcut = null
            }
        )
    }
}
