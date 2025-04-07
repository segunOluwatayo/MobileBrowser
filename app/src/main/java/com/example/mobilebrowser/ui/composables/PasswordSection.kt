package com.example.mobilebrowser.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.mobilebrowser.data.entity.PasswordEntity
import com.example.mobilebrowser.ui.viewmodels.PasswordViewModel
import kotlinx.coroutines.launch

@Composable
fun PasswordSection(
    passwordViewModel: PasswordViewModel,
    onRequestAuthentication: (PasswordEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Observe the password list from the ViewModel
    val passwordList by passwordViewModel.passwords.collectAsState()
    var selectedPassword by remember { mutableStateOf<PasswordEntity?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // This callback can be used to trigger authentication (biometric/passcode)
    fun handlePasswordClick(password: PasswordEntity) {
        // Instead of directly showing details, invoke your authentication flow
        onRequestAuthentication(password)
        // Optionally, if authentication succeeds, set selectedPassword and show the dialog:
        // selectedPassword = password
        // showPasswordDialog = true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Saved Passwords",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (passwordList.isEmpty()) {
            Text(
                text = "No passwords saved",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn {
                items(passwordList) { password ->
                    PasswordItem(password = password, onClick = { handlePasswordClick(password) })
                }
            }
        }
    }

    // Password detail dialog, shown after authentication succeeds
    if (showPasswordDialog && selectedPassword != null) {
        PasswordDetailDialog(
            password = selectedPassword!!,
            onDismiss = {
                showPasswordDialog = false
                selectedPassword = null
            },
            passwordViewModel = passwordViewModel
        )
    }
}

@Composable
fun PasswordItem(password: PasswordEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = password.siteUrl, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Username: ${password.username}", style = MaterialTheme.typography.bodyMedium)
            // We don't display the password here; it is revealed after authentication.
        }
    }
}

@Composable
fun PasswordDetailDialog(
    password: PasswordEntity,
    onDismiss: () -> Unit,
    passwordViewModel: PasswordViewModel
) {
    var decryptedPassword by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Launch decryption when the dialog appears
    LaunchedEffect(password) {
        decryptedPassword = passwordViewModel.getDecryptedPassword(password.encryptedPassword)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Password Details") },
        text = {
            Column {
                Text(text = "Site: ${password.siteUrl}")
                Text(text = "Username: ${password.username}")
                Text(text = "Password: $decryptedPassword")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
