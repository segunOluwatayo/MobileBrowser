package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.ui.composables.SearchEngine
import com.example.mobilebrowser.ui.viewmodels.SettingsViewModel
import com.example.mobilebrowser.R

/**
 * SearchEngineSelectionScreen displays a list of available search engines.
 *
 * When an engine is selected, it updates the setting via the view model and
 * navigates back.
 *
 * @param onNavigateBack Callback invoked when the user taps the back arrow or after selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchEngineSelectionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // Define the list of available search engines.
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Search Engine") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            items(searchEngines) { engine ->
                OutlinedCard(
                    onClick = {
                        // Update the selected search engine.
                        viewModel.updateSearchEngine(engine.searchUrl)
                        // Navigate back to the settings screen.
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    ListItem(
                        headlineContent = { Text(engine.name) },
                        leadingContent = {
                            Icon(
                                painter = painterResource(id = engine.iconRes),
                                contentDescription = engine.name
                            )
                        }
                    )
                }
            }
        }
    }
}
