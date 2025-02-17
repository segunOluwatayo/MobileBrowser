package com.example.mobilebrowser.ui.homepage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.data.entity.ShortcutEntity
import com.example.mobilebrowser.ui.screens.HomeScreen
import com.example.mobilebrowser.ui.viewmodels.ShortcutViewModel

@Composable
fun HomeScreenWithViewModel(
    viewModel: ShortcutViewModel = hiltViewModel()
) {
    // Collect the current list of ShortcutEntity items from the ViewModel
    val shortcuts by viewModel.shortcuts.collectAsState()

    // Render the HomeScreen with the shortcuts data and event handlers
    HomeScreen(
        shortcuts = shortcuts,
        onShortcutClick = { shortcut ->
            viewModel.onShortcutClick(shortcut)
        },
        onShortcutLongPressed = { shortcut ->
            viewModel.onShortcutLongPress(shortcut)
        }
    )
}