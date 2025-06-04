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
fun ThemeSelectionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // Observe the current theme mode from the view model.
    val themeMode by viewModel.themeMode.collectAsState()

    val options = listOf("SYSTEM", "LIGHT", "DARK")
    val displayNames = mapOf(
        "SYSTEM" to "System Default",
        "LIGHT" to "Light Mode",
        "DARK" to "Dark Mode"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme Selection") },
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
                text = "Select a Theme",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Iterate through the options to display each as a selectable row.
            options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateThemeMode(option) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (themeMode == option),
                        onClick = { viewModel.updateThemeMode(option) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = displayNames[option] ?: option,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
