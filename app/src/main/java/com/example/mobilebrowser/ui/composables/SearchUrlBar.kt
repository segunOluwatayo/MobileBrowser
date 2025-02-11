package com.example.mobilebrowser.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.mobilebrowser.R

// Handler object for processing and formatting URL strings
object UrlDisplayHandler {
    fun extractSearchQuery(url: String): String? {
        return when {
            // Check if URL is from Google search
            url.contains("google.com/search?") -> {
                // Extract query parameter 'q' and replace '+' with space
                url.substringAfter("q=").substringBefore("&").replace("+", " ")
            }
            // Check if URL is from Bing search
            url.contains("bing.com/search?") -> {
                url.substringAfter("q=").substringBefore("&").replace("+", " ")
            }
            // Check if URL is from DuckDuckGo search
            url.contains("duckduckgo.com/?") -> {
                url.substringAfter("q=").substringBefore("&").replace("+", " ")
            }
            // Check if URL is from Qwant search
            url.contains("qwant.com/?") -> {
                url.substringAfter("q=").substringBefore("&").replace("+", " ")
            }
            // Check if URL is from Wikipedia search
            url.contains("wikipedia.org/wiki/Special:Search") -> {
                url.substringAfter("search=").substringBefore("&").replace("+", " ")
            }
            // Check if URL is from eBay search
            url.contains("ebay.com/sch/") -> {
                url.substringAfter("_nkw=").substringBefore("&").replace("+", " ")
            }
            // Return null if none of the patterns match
            else -> null
        }
    }

    fun getDisplayText(url: String, isEditing: Boolean): String {
        // When editing, return the raw URL text
        if (isEditing) return url

        // Attempt to extract a search query from the URL
        val searchQuery = extractSearchQuery(url)
        if (searchQuery != null) {
            return searchQuery
        }

        // Remove common URL prefixes and suffixes for cleaner display
        return url.removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removeSuffix("/")
    }
}

// Data class representing a search engine with its properties
data class SearchEngine(
    val name: String,
    val searchUrl: String,
    val iconRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchUrlBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: (String, SearchEngine) -> Unit,
    onNavigate: (String) -> Unit,
    isEditing: Boolean,
    modifier: Modifier = Modifier
) {
    // State to control whether the dropdown menu is shown
    var showDropdown by remember { mutableStateOf(false) }

    // List of available search engines. This list is remembered across recompositions.
    val searchEngines = remember {
        listOf(
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
    }

    // State to keep track of the currently selected search engine; default to the first one
    var selectedEngine by remember { mutableStateOf(searchEngines[0]) }

    // Container box for the URL bar and dropdown menu
    Box(modifier = modifier) {

        // Outlined text field for URL input or search query
        OutlinedTextField(
            // Display text is formatted based on whether the field is being edited
            value = UrlDisplayHandler.getDisplayText(value, isEditing),
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp), // Rounded corners for a smooth appearance
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            ),
            // Leading icon area displaying the selected search engine's icon and dropdown arrow
            leadingIcon = {
                Row(
                    modifier = Modifier
                        .clickable { showDropdown = true } // Open dropdown on click
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Display the search engine icon
                    Icon(
                        painter = painterResource(id = selectedEngine.iconRes),
                        contentDescription = selectedEngine.name,
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified // Use the original colors of the icon
                    )
                    // Dropdown arrow icon to indicate menu expansion
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select search engine",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            // Placeholder text when the field is empty
            placeholder = { Text("Search or enter address") },
            singleLine = true,
            // Configure the keyboard options: show a "Search" action on the keyboard
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            // Define what happens when the search action is triggered on the keyboard
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = {
                    // Determine if the input is likely a URL (contains a dot and no spaces)
                    if (value.contains(".") && !value.contains(" ")) {
                        var url = value
                        // Prepend "https://" if the URL doesn't start with a valid scheme
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://$url"
                        }
                        // Navigate to the specified URL
                        onNavigate(url)
                    } else {
                        // Otherwise, perform a search using the selected search engine
                        onSearch(value, selectedEngine)
                    }
                }
            )
        )

        // Dropdown menu for selecting a search engine and accessing extra options
        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            modifier = Modifier
                .width(280.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Title for the dropdown menu
            Text(
                text = "This time search in:",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium
            )

            // List each search engine as a selectable item in the dropdown
            searchEngines.forEach { engine ->
                DropdownMenuItem(
                    text = { Text(engine.name) },
                    leadingIcon = {
                        // Icon for each search engine
                        Icon(
                            painter = painterResource(id = engine.iconRes),
                            contentDescription = engine.name,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified // Keep the icon's original colors
                        )
                    },
                    onClick = {
                        // Update the selected engine and close the dropdown when an item is clicked
                        selectedEngine = engine
                        showDropdown = false
                    }
                )
            }

            // A divider to separate search engine options from extra options
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Extra dropdown menu items for additional actions (Bookmarks, History, and Settings)
            DropdownMenuItem(
                text = { Text("Bookmarks") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Bookmarks"
                    )
                },
                onClick = { showDropdown = false } // Currently only closes the dropdown
            )

            DropdownMenuItem(
                text = { Text("History") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History"
                    )
                },
                onClick = { showDropdown = false }
            )

            DropdownMenuItem(
                text = { Text("Search settings") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                },
                onClick = { showDropdown = false }
            )
        }
    }
}
