package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomepageSelectionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // Observe the current visibility settings from the view model
    val homepageEnabled by viewModel.homepageEnabled.collectAsState()
    val recentTabEnabled by viewModel.recentTabEnabled.collectAsState()
    val bookmarksEnabled by viewModel.bookmarksEnabled.collectAsState()
    val historyEnabled by viewModel.historyEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Homepage Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Show on Homepage",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Shortcuts toggle
            SettingToggle(
                title = "Shortcuts",
                description = "Show pinned shortcuts and frequently visited sites",
                isChecked = homepageEnabled,
                onCheckedChange = { viewModel.updateHomepageEnabled(it) }
            )

            // Recent tab toggle
            SettingToggle(
                title = "Resume Browsing",
                description = "Show your recent tab for quick access",
                isChecked = recentTabEnabled,
                onCheckedChange = { viewModel.updateRecentTabEnabled(it) }
            )

            // Bookmarks toggle
            SettingToggle(
                title = "Bookmarks",
                description = "Show your saved bookmarks",
                isChecked = bookmarksEnabled,
                onCheckedChange = { viewModel.updateBookmarksEnabled(it) }
            )

            // History toggle
            SettingToggle(
                title = "Recently Visited",
                description = "Show websites you recently visited",
                isChecked = historyEnabled,
                onCheckedChange = { viewModel.updateHistoryEnabled(it) }
            )
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}
