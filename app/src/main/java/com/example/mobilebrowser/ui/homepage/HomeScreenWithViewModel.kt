package com.example.mobilebrowser.ui.homepage

import Shortcut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.data.entity.ShortcutEntity
import com.example.mobilebrowser.ui.screens.HomeScreen
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
        onShortcutClick = { shortcut ->
            // Handle normal click (e.g., open the URL in current tab)
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
            // Handle long press: call the ViewModel for a long press.
            viewModel.onShortcutLongPress(
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
