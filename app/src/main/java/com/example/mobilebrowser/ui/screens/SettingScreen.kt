package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.ui.composables.SearchEngine
import com.example.mobilebrowser.ui.viewmodels.SettingsViewModel
import com.example.mobilebrowser.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSelectSearchEngine: () -> Unit,
    onSelectTabManagement: () -> Unit,
    onSelectTheme: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val searchEngines = listOf(
        SearchEngine(
            name = "Google",
            searchUrl = "https://www.google.com/search?q=",
            iconRes = R.drawable.google_icon
        ),
        SearchEngine(
            name = "Bing",
            searchUrl = "https://www.bing.com/search?q=",
            iconRes = R.drawable.bing_icon
        ),
        SearchEngine(
            name = "DuckDuckGo",
            searchUrl = "https://duckduckgo.com/?q=",
            iconRes = R.drawable.duckduckgo_icon
        ),
        SearchEngine(
            name = "Qwant",
            searchUrl = "https://www.qwant.com/?q=",
            iconRes = R.drawable.qwant_icon
        ),
        SearchEngine(
            name = "Wikipedia",
            searchUrl = "https://wikipedia.org/wiki/Special:Search?search=",
            iconRes = R.drawable.wikipedia_icon
        ),
        SearchEngine(
            name = "eBay",
            searchUrl = "https://www.ebay.com/sch/i.html?_nkw=",
            iconRes = R.drawable.ebay_icon
        )
    )

    val currentEngineUrl by viewModel.searchEngine.collectAsState()
    val currentEngine = searchEngines.find { it.searchUrl == currentEngineUrl } ?: searchEngines[0]
    val currentTabPolicy by viewModel.tabManagementPolicy.collectAsState()
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val homepageEnabled by viewModel.homepageEnabled.collectAsState()

    // Convert the theme mode value to a user-friendly string.
    val themeDisplayName = when (currentThemeMode) {
        "LIGHT" -> "Light Mode"
        "DARK" -> "Dark Mode"
        else -> "System Default"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(horizontal = 16.dp)
        ) {
            // Section heading
            Text(
                text = "General",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // Search Engine setting
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSearchEngine() }
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Search Engine",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentEngine.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // Tab Management Policy section
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTabManagement() }
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Tab Management Policy",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Map the policy value to a friendly display string.
                                    val displayPolicy = when (currentTabPolicy) {
                                        "MANUAL" -> "Manually"
                                        "ONE_DAY" -> "After One Day"
                                        "ONE_WEEK" -> "After One Week"
                                        "ONE_MONTH" -> "After One Month"
                                        else -> "Manually"
                                    }
                                    Text(
                                        text = displayPolicy,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Theme Selection section
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTheme() }
                    ) {
                        Column(modifier = Modifier.padding(vertical = 16.dp)) {
                            Text(
                                text = "Theme",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = themeDisplayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Homepage setting section (Shortcuts)
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Shortcuts",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "When on, will show your pinned shortcuts (and, eventually, dynamic shortcut suggestions based on your browsing habits).",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = homepageEnabled,
                                onCheckedChange = { viewModel.updateHomepageEnabled(it) }
                            )
                        }
                    }


                    // Divider line
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}