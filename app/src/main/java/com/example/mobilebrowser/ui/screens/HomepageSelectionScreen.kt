package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.clickable
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

    // Define two options: Enabled (true) and Disabled (false).
    val options = listOf(true, false)
    val displayNames = mapOf(
        true to "Enabled",
        false to "Disabled"
    )

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
                .padding(16.dp)
        ) {
            Text(
                text = "Display Shortcuts on New Tabs",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Display options as radio buttons
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateHomepageEnabled(option) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (homepageEnabled == option),
                        onClick = { viewModel.updateHomepageEnabled(option) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayNames[option] ?: option.toString(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "When enabled, your homepage will show your pinned shortcuts (and eventually, dynamic shortcut suggestions based on your browsing habits).",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
