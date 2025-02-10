package com.example.mobilebrowser.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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

/**
 * A composable that displays a custom search bar with a search engine selection dropdown.
 *
 * The search bar consists of:
 * - An icon button showing the current search engine's logo. Tapping it toggles the dropdown.
 * - A text field for entering a search query with a placeholder.
 * - A dropdown menu listing all available search engines.
 *
 * @param onSearchSubmit Callback that is triggered when the user submits a search.
 *        It receives the constructed search URL as its parameter.
 * @param searchViewModel The [SearchViewModel] providing state and actions for the search bar.
 *        Defaults to a Hilt-injected instance.
 */
@Composable
fun SearchBar(
    onSearchSubmit: (String) -> Unit,
    searchViewModel: SearchViewModel = hiltViewModel()
) {
    // Collect the necessary state from the view model.
    val currentSearchEngine by searchViewModel.currentSearchEngine.collectAsState()
    val searchQuery by searchViewModel.searchQuery.collectAsState()
    val isDropdownVisible by searchViewModel.isDropdownVisible.collectAsState()
    val searchEngines by searchViewModel.searchEngines.collectAsState()

    // Controller to manage the software keyboard.
    val keyboardController = LocalSoftwareKeyboardController.current

    // Row containing the search engine icon and the search text field.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Button for the current search engine.
        IconButton(
            onClick = { searchViewModel.toggleDropdownVisibility() }
        ) {
            Icon(
                painter = painterResource(id = currentSearchEngine.logo),
                contentDescription = "Select search engine"
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // TextField for entering search queries.
        TextField(
            value = searchQuery,
            onValueChange = { searchViewModel.setSearchQuery(it) },
            placeholder = { Text("Search or enter address") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = {
                    // Construct the search URL by appending the query to the selected engine's base URL.
                    val searchUrl = currentSearchEngine.baseUrl + searchQuery
                    // Trigger the search submission callback.
                    onSearchSubmit(searchUrl)
                    // Hide the software keyboard.
                    keyboardController?.hide()
                }
            )
        )
    }

    // Dropdown menu listing available search engines.
    DropdownMenu(
        expanded = isDropdownVisible,
        onDismissRequest = { searchViewModel.setDropdownVisibility(false) }
    ) {
        searchEngines.forEach { engine ->
            DropdownMenuItem(
                onClick = {
                    // Update the current search engine when an item is selected.
                    searchViewModel.selectSearchEngine(engine)
                    // Hide the dropdown after selection.
                    searchViewModel.setDropdownVisibility(false)
                },
                text = { Text(engine.name) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = engine.logo),
                        contentDescription = engine.name
                    )
                }
            )
        }
    }
}

