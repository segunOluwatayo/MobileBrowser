package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.ui.viewmodels.SettingsViewModel
import com.example.mobilebrowser.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEngineSelectionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // Default search engines
    val defaultSearchEngines = listOf(
        SearchEngine(
            name = "DuckDuckGo",
            domain = "duckduckgo.com",
            searchUrl = "https://duckduckgo.com/?q=",
            iconRes = R.drawable.duckduckgo_icon
        ),
        SearchEngine(
            name = "eBay",
            domain = "ebay.com",
            searchUrl = "https://www.ebay.com/sch/i.html?_nkw=",
            iconRes = R.drawable.ebay_icon
        ),
        SearchEngine(
            name = "Google",
            domain = "google.com",
            searchUrl = "https://www.google.com/search?q=",
            iconRes = R.drawable.google_icon
        ),
        SearchEngine(
            name = "Microsoft Bing",
            domain = "bing.com",
            searchUrl = "https://www.bing.com/search?q=",
            iconRes = R.drawable.bing_icon
        ),
        SearchEngine(
            name = "Qwant",
            domain = "qwant.com",
            searchUrl = "https://www.qwant.com/?q=",
            iconRes = R.drawable.qwant_icon
        ),
        SearchEngine(
            name = "Wikipedia",
            domain = "wikipedia.org",
            searchUrl = "https://wikipedia.org/wiki/Special:Search?search=",
            iconRes = R.drawable.wikipedia_icon
        )
    )

    // Retrieve custom engines from the ViewModel.
    val customEngines by viewModel.customSearchEngines.collectAsState()
    // Currently selected search engine URL.
    val currentEngineUrl by viewModel.searchEngine.collectAsState()

    // State to show/hide the add custom engine dialog.
    var showAddDialog by remember { mutableStateOf(false) }

    // Merge default and custom engines; for custom ones, use a generic icon.
    val mergedSearchEngines = (defaultSearchEngines + customEngines.map { custom ->
        SearchEngine(
            name = custom.name,
            domain = "", // custom engines may not have a domain value
            searchUrl = custom.searchUrl,
            iconRes = R.drawable.generic_searchengine
        )
    }).sortedBy { it.name }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Search engine",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)
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
            items(mergedSearchEngines) { engine ->
                ListItem(
                    headlineContent = {
                        Text(engine.name, style = MaterialTheme.typography.bodyLarge)
                    },
                    supportingContent = {
                        Text(
                            if (engine.domain.isNotEmpty()) engine.domain else engine.searchUrl,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    },
                    leadingContent = {
                        Image(
                            painter = painterResource(id = engine.iconRes),
                            contentDescription = engine.name,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    trailingContent = {
                        RadioButton(
                            selected = engine.searchUrl == currentEngineUrl,
                            onClick = { viewModel.updateSearchEngine(engine.searchUrl) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    },
                    modifier = Modifier.clickable {
                        viewModel.updateSearchEngine(engine.searchUrl)
                    }
                )
                Divider()
            }
            // "Add Custom" button at the end of the list.
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Add Custom")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Show the add custom engine dialog when requested.
    if (showAddDialog) {
        AddSearchEngineDialog(
            onDismiss = { showAddDialog = false },
            onAddEngine = { name, url ->
                viewModel.addCustomSearchEngine(name, url)
                showAddDialog = false
            },
            errorMessage = viewModel.customEngineErrorMessage.collectAsState().value
        )
    }
}

// Data class representing a search engine in the UI.
data class SearchEngine(
    val name: String,
    val domain: String,
    val searchUrl: String,
    val iconRes: Int
)
