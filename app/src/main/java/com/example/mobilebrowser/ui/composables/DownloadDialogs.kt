package com.example.mobilebrowser.ui.composables

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DownloadProgressIndicator(
    progress: Int,
    filename: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = filename,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                text = "$progress%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PermissionRequest(
    rationale: String,
    onRequestPermission: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Do nothing, force user to make a choice */ },
        title = { Text("Permission Required") },
        text = { Text(rationale) },
        confirmButton = {
            Button(onClick = onRequestPermission) {
                Text("Grant Permission")
            }
        }
    )
}

@Composable
fun FileTypeNotSupportedDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Type Not Supported") },
        text = {
            Text("This type of file cannot be opened directly. " +
                    "You may need to install a compatible app to open this file.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}