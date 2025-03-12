package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.mobilebrowser.data.entity.CustomSearchEngine

@Composable
fun EditSearchEngineDialog(
    engine: CustomSearchEngine,
    onDismiss: () -> Unit,
    onUpdateEngine: (String, String) -> Unit,
    errorMessage: String? = null
) {
    // Local state for the engine name and URL, initialized with current values
    var engineName by remember(engine) { mutableStateOf(engine.name) }
    var engineUrl by remember(engine) { mutableStateOf(engine.searchUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Edit Search Engine")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = engineName,
                    onValueChange = { engineName = it },
                    label = { Text("Engine Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = engineUrl,
                    onValueChange = { engineUrl = it },
                    label = { Text("Search URL") },
                    placeholder = { Text("https://example.com/search?q=%s") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (engineName.isNotBlank() && engineUrl.isNotBlank()) {
                                onUpdateEngine(engineName, engineUrl)
                            }
                        }
                    )
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onUpdateEngine(engineName, engineUrl) },
                enabled = engineName.isNotBlank() && engineUrl.isNotBlank()
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}