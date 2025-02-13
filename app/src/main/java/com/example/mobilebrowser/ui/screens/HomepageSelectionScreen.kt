package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
    // Observe the current homepage (shortcuts) setting from the view model.
    val homepageEnabled by viewModel.homepageEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shortcuts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                text = "Display Shortcuts on Homepage",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Toggle Switch row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Shortcut",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = homepageEnabled,
                    onCheckedChange = { viewModel.updateHomepageEnabled(it) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "When enabled, your homepage will show your pinned shortcuts (and eventually, dynamic shortcut suggestions based on your browsing habits).",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
