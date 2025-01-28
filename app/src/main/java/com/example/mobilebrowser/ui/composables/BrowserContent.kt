package com.example.mobilebrowser.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController // Import this
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.text.input.ImeAction // Import this

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserContent(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    currentUrl: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        var urlText by remember { mutableStateOf(currentUrl) }
        val softwareKeyboardController = LocalSoftwareKeyboardController.current // Get keyboard controller

        // Navigation bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick = onBack,
                enabled = canGoBack
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
            }

            // Forward button
            IconButton(
                onClick = onForward,
                enabled = canGoForward
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Go forward")
            }

            // Reload button
            IconButton(onClick = onReload) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload")
            }

            // URL bar
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                singleLine = true,
                label = { Text("Enter URL") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go), // Use KeyboardOptions
                keyboardActions = KeyboardActions( // Use KeyboardActions
                    onGo = { // Define action for ImeAction.Go
                        onNavigate(urlText)
                        softwareKeyboardController?.hide()
                    }
                )
            )
        }

        // GeckoView component - Moved OUTSIDE and BELOW the Row
        GeckoViewComponent(
            url = currentUrl,
            onUrlChange = onNavigate,
            onCanGoBackChange = { canGoBack -> /* Update state - will be handled in MainActivity */ },
            onCanGoForwardChange = { canGoForward -> /* Update state - will be handled in MainActivity */ },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}