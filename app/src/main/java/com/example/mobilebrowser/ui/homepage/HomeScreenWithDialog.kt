package com.example.mobilebrowser.ui.homepage

import Shortcut
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.data.entity.ShortcutEntity
import com.example.mobilebrowser.ui.screens.HomeScreen
import com.example.mobilebrowser.ui.homepage.ShortcutOptionsDialog
import com.example.mobilebrowser.ui.viewmodels.ShortcutViewModel

@Composable
fun HomeScreenWithDialog(viewModel: ShortcutViewModel = hiltViewModel()) {
    // Observe the list of shortcuts from the ViewModel
    val shortcuts by viewModel.shortcuts.collectAsState()

    // State variable to track which shortcut is selected (for long press)
    var selectedShortcut by remember { mutableStateOf<ShortcutEntity?>(null) }

    // Render the HomeScreen
    HomeScreen(
        shortcuts = shortcuts,
        onShortcutClick = { shortcut ->
            viewModel.onShortcutClick(shortcut)
        },
        onShortcutLongPressed = { shortcut ->
            selectedShortcut = shortcut
        }
    )

    // If a shortcut is selected, display the options dialog
    selectedShortcut?.let { shortcut ->
        ShortcutOptionsDialog(
            shortcut = shortcut,
            onDismiss = { selectedShortcut = null },
            onOpenInNewTab = {
                // Handle open in new tab
                selectedShortcut = null
            },
            onEdit = {
                // Handle edit
                selectedShortcut = null
            },
            onTogglePin = {
                viewModel.togglePin(shortcut)
                selectedShortcut = null
            },
            onDelete = {
                viewModel.deleteShortcut(shortcut)
                selectedShortcut = null
            }
        )
    }
}
