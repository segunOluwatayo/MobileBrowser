package com.example.mobilebrowser.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.ui.viewmodels.SearchViewModel

@Composable
fun SearchBar(
    onSearchSubmit: (String) -> Unit,
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    val currentSearchEngine by searchViewModel.currentSearchEngine.collectAsState()
    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val isDropdownVisible by searchViewModel.isDropdownVisible.collectAsState()
    val searchEngines by searchViewModel.searchEngines.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search Engine Icon (Clickable)
        IconButton(
            onClick = { searchViewModel.toggleDropdownVisibility() } // Toggle dropdown visibility
        ) {
            Icon(
                painter = painterResource(id = currentSearchEngine.logo),
                contentDescription = "Select search engine",
                modifier = Modifier
                    .size(24.dp) // Adjust size as needed
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Search Input Field
        TextField(
            value = searchQuery,
            onValueChange = { searchViewModel.setSearchQuery(it) },
            placeholder = { Text("Search or enter address") },
            modifier = Modifier.weight(1f), // Take remaining space
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go), // Use ImeAction.Go
            keyboardActions = KeyboardActions(
                onGo = {
                    val searchUrl = currentSearchEngine.baseUrl + searchQuery
                    onSearchSubmit(searchUrl)  // Call the provided callback
                    keyboardController?.hide() // Hide keyboard
                }
            ),
            // Optional:  Add a clear button to the TextField (trailingIcon)
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Clear,
                        contentDescription = "Clear Search",
                        modifier = Modifier.clickable { searchViewModel.clearSearchQuery() }
                    )
                }
            }
        )
        // No Spacer needed here: TextField takes remaining space due to weight(1f)
    }

    // Search Engine Dropdown
    DropdownMenu(
        expanded = isDropdownVisible,
        onDismissRequest = { searchViewModel.setDropdownVisibility(false) } // Hide dropdown
    ) {
        searchEngines.forEach { engine ->
            DropdownMenuItem(
                onClick = {
                    searchViewModel.selectSearchEngine(engine) // Select the engine
                    searchViewModel.setDropdownVisibility(false) // Hide dropdown
                },
                text = { Text(engine.name) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = engine.logo),
                        contentDescription = engine.name,
                        modifier = Modifier.size(24.dp) // Consistent icon size
                    )
                }
            )
        }
    }
}