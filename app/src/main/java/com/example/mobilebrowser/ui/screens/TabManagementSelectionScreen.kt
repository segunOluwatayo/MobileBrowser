package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.ui.viewmodels.SettingsViewModel

// Data class representing a tab management policy option.
data class TabPolicy(val value: String, val displayName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabManagementSelectionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // Define the list of available tab management policies.
    val policies = listOf(
        TabPolicy("MANUAL", "Manually"),
        TabPolicy("ONE_DAY", "After One Day"),
        TabPolicy("ONE_WEEK", "After One Week"),
        TabPolicy("ONE_MONTH", "After One Month")
    )

    // Collect the current tab management policy from the view model.
    val currentPolicy by viewModel.tabManagementPolicy.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tab Management Policy",
                        style = MaterialTheme.typography.titleLarge
                    )
                },

                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            item {
                Text(
                    text = "Close tabs",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }

            items(policies) { policy ->
                ListItem(
                    headlineContent = {
                        Text(text = policy.displayName, style = MaterialTheme.typography.bodyLarge)
                    },
                    trailingContent = {
                        RadioButton(
                            selected = (policy.value == currentPolicy),
                            onClick = {
                                viewModel.updateTabManagementPolicy(policy.value)
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    modifier = Modifier.clickable {
                        viewModel.updateTabManagementPolicy(policy.value)
                    }
                )
                // Add a divider between items except for the last one.
                if (policy != policies.last()) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}
