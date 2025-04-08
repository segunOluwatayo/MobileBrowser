package com.example.mobilebrowser.ui.composables

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

/**
 * A dialog that asks the user if they would like to save their credentials.
 *
 * @param siteUrl The website URL where the credentials were used.
 * @param username The username entered by the user.
 * @param plainPassword The plain text password to be encrypted and saved.
 * @param onDismiss Called when the dialog is dismissed.
 * @param onSave A suspend function that handles the saving process.
 */
@Composable
fun PasswordSaveDialog(
    siteUrl: String,
    username: String,
    plainPassword: String,
    onDismiss: () -> Unit,
    onSave: suspend (siteUrl: String, username: String, plainPassword: String) -> Unit
) {
    val scope = rememberCoroutineScope()

    // A helper to extract just the domain from a full URL
    fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(if (url.startsWith("http")) url else "https://$url")
            var domain = uri.host ?: url
            if (domain.startsWith("www.")) domain = domain.substring(4)
            domain
        } catch (e: Exception) {
            url
        }
    }

    val domain = extractDomain(siteUrl)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Save Credentials") },
        text = {
            Text(text = "Would you like to save your username and password for $domain?")
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    // Pass the domain instead of the full path
                    onSave(domain, username, plainPassword)
                    onDismiss()
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}
