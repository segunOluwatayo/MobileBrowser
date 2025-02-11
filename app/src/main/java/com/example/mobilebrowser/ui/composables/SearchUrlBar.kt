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
    var showDropdown by remember { mutableStateOf(false) }

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

    var selectedEngine by remember { mutableStateOf(searchEngines[0]) }

    // Display text logic updated to keep search queries visible
    val displayText = when {
        isEditing -> value  // Show text when editing
        value.contains("mozilla.org") -> ""  // Hide mozilla.org URL
        value.isNotBlank() -> value  // Show any other non-empty value
        else -> ""  // Show empty for blank values
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayText,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            ),
            leadingIcon = {
                Row(
                    modifier = Modifier
                        .clickable { showDropdown = true }
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = selectedEngine.iconRes),
                        contentDescription = selectedEngine.name,
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select search engine",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            placeholder = {
                Text(
                    "Search or enter address",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = {
                    val input = value.trim()
                    if (input.isBlank()) return@KeyboardActions

                    if (input.contains(".") && !input.contains(" ")) {
                        var url = input
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://$url"
                        }
                        onNavigate(url)
                    } else {
                        onSearch(input, selectedEngine)
                    }
                }
            )
        )

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            modifier = Modifier
                .width(280.dp)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Text(
                text = "Search with:",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium
            )

            searchEngines.forEach { engine ->
                DropdownMenuItem(
                    text = { Text(engine.name) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = engine.iconRes),
                            contentDescription = engine.name,
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                    },
                    onClick = {
                        selectedEngine = engine
                        showDropdown = false
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            DropdownMenuItem(
                text = { Text("Bookmarks") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Bookmarks"
                    )
                },
                onClick = { showDropdown = false }
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