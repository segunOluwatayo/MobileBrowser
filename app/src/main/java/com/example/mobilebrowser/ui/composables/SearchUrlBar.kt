package com.example.mobilebrowser.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
    currentSearchEngine: SearchEngine,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    var tempSelectedEngine by remember { mutableStateOf(currentSearchEngine) }

    LaunchedEffect(currentSearchEngine) {
        tempSelectedEngine = currentSearchEngine
    }

    val displayText = when {
        isEditing -> value
        value.contains("mozilla.org") -> ""
        value.isNotBlank() -> value
        else -> ""
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayText,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            ),
            leadingIcon = {
                Row(
                    modifier = Modifier
                        .clickable { showDropdown = true }
                        .padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        painter = painterResource(id = tempSelectedEngine.iconRes),
                        contentDescription = tempSelectedEngine.name,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 4.dp),
                        tint = Color.Unspecified
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Select search engine",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            placeholder = {
                Text(
                    "Search or enter address",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
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
                        onSearch(input, tempSelectedEngine)
                    }
                    tempSelectedEngine = currentSearchEngine
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

            val searchEngines = listOf(
                SearchEngine("Google", "https://www.google.com/search?q=", R.drawable.google_icon),
                SearchEngine("Bing", "https://www.bing.com/search?q=", R.drawable.bing_icon),
                SearchEngine("DuckDuckGo", "https://duckduckgo.com/?q=", R.drawable.duckduckgo_icon),
                SearchEngine("Qwant", "https://www.qwant.com/?q=", R.drawable.qwant_icon),
                SearchEngine("Wikipedia", "https://wikipedia.org/wiki/Special:Search?search=", R.drawable.wikipedia_icon),
                SearchEngine("eBay", "https://www.ebay.com/sch/i.html?_nkw=", R.drawable.ebay_icon)
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
                        tempSelectedEngine = engine
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