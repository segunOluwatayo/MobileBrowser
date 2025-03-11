package com.example.mobilebrowser.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
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
    currentUrl: String,
    onValueChange: (String) -> Unit,
    onSearch: (String, SearchEngine) -> Unit,
    onNavigate: (String) -> Unit,
    isEditing: Boolean,
    currentSearchEngine: SearchEngine,
    onStartEditing: () -> Unit,
    onEndEditing: () -> Unit,
    availableSearchEngines: List<SearchEngine>, // New parameter for merged engines
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    var tempSelectedEngine by remember(currentSearchEngine) { mutableStateOf(currentSearchEngine) }
    val focusManager = LocalFocusManager.current

    // Determine which text to display.
    val displayValue = when {
        isEditing -> value
        currentUrl.isNotBlank() -> currentUrl
        else -> ""
    }

    LaunchedEffect(currentSearchEngine) {
        tempSelectedEngine = currentSearchEngine
    }

    LaunchedEffect(isEditing) {
        if (!isEditing) {
            focusManager.clearFocus()
        }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(56.dp)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onStartEditing()
                        if (!isEditing && currentUrl.isNotBlank()) {
                            onValueChange(currentUrl)
                        }
                    } else if (!focusState.hasFocus) {
                        onEndEditing()
                    }
                },
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary,
                selectionColors = TextSelectionColors(
                    handleColor = MaterialTheme.colorScheme.primary,
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
            ),
            leadingIcon = {
                Row(
                    modifier = Modifier
                        .clickable { showDropdown = true }
                        .padding(start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        painter = painterResource(id = tempSelectedEngine.iconRes),
                        contentDescription = tempSelectedEngine.name,
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 4.dp),
                        tint = Color.Unspecified
                    )
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Select search engine",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            placeholder = {
                if (displayValue.isEmpty()) {
                    Text(
                        "Search or enter address",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
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
                    focusManager.clearFocus()
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
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Use the provided list instead of a hard-coded one.
            availableSearchEngines.forEach { engine ->
                DropdownMenuItem(
                    text = {
                        Text(engine.name, color = MaterialTheme.colorScheme.onSurface)
                    },
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
            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            DropdownMenuItem(
                text = { Text("Bookmarks", color = MaterialTheme.colorScheme.onSurface) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "Bookmarks",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = { showDropdown = false }
            )
            DropdownMenuItem(
                text = { Text("History", color = MaterialTheme.colorScheme.onSurface) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = { showDropdown = false }
            )
            DropdownMenuItem(
                text = { Text("Search settings", color = MaterialTheme.colorScheme.onSurface) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = { showDropdown = false }
            )
        }
    }
}