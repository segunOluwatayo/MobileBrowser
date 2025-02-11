package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.ui.composables.SearchEngine
import com.example.mobilebrowser.ui.viewmodels.SettingsViewModel
import com.example.mobilebrowser.R

/**
 * SettingsScreen displays the app settings.
 *
 * It features a TopAppBar with a back arrow and a "General" section that
 * includes the "Search Engine" setting row. When tapped, it navigates to a
 * dedicated SearchEngineSelectionScreen.
 *
 * @param onNavigateBack Callback invoked when the user taps the back arrow.
 * @param onSelectSearchEngine Callback invoked when the user wants to change the search engine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSelectSearchEngine: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // Define the list of available search engines.
    // (This list is reused to map the persisted value to a SearchEngine object.)
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

    // Observe the persisted search engine URL.
    val currentEngineUrl by viewModel.searchEngine.collectAsState()
    // Map the persisted URL to a SearchEngine object; default to Google if not found.
    val currentEngine = searchEngines.find { it.searchUrl == currentEngineUrl } ?: searchEngines[0]

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
                .padding(16.dp)
        ) {
            // Section heading.
            Text(
                text = "General",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // "Search Engine" setting row.
            OutlinedCard(
                onClick = { onSelectSearchEngine() },
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("Search Engine") },
                    supportingContent = { Text(currentEngine.name) },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Select search engine"
                        )
                    }
                )
            }
        }
    }
}
