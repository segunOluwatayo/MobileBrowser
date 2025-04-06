package com.example.mobilebrowser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilebrowser.sync.SyncStatusState
import com.example.mobilebrowser.ui.composables.SyncStatusIndicator
import com.example.mobilebrowser.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUrl: (String) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val isSignedIn by viewModel.isSignedIn.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()

    // Sync preferences
    val syncHistory by viewModel.syncHistoryEnabled.collectAsState()
    val syncBookmarks by viewModel.syncBookmarksEnabled.collectAsState()
    val syncTabs by viewModel.syncTabsEnabled.collectAsState()

    var showSignOutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Format the last sync timestamp into a readable string.
    val lastSyncText = lastSyncTimestamp?.let { timestamp ->
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        "Last Sync: ${sdf.format(Date(timestamp))}"
    } ?: "Never Synced"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            if (isSignedIn) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .padding(4.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .weight(1f)
                            ) {
                                Text(
                                    text = userName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sync status section with actual data.
//                        Column(
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Text(
//                                text = when (syncStatus) {
//                                    is SyncStatusState.Idle -> "Sync Status: Idle"
//                                    is SyncStatusState.Syncing -> "Sync Status: Syncing..."
//                                    is SyncStatusState.Synced -> "Sync Status: Synced"
//                                    is SyncStatusState.Error -> "Sync Status: Error"
//                                },
//                                style = MaterialTheme.typography.bodyMedium
//                            )
//                            Text(
//                                text = lastSyncText,
//                                style = MaterialTheme.typography.bodySmall,
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                            OutlinedButton(
//                                onClick = { viewModel.performInitialSync() },
//                                modifier = Modifier.padding(top = 8.dp)
//                            ) {
//                                Text("Sync Now")
//                            }
//                        }
                        SyncStatusIndicator(
                            syncStatus = syncStatus,
                            lastSyncTimestamp = lastSyncTimestamp,
                            onSyncClicked = { viewModel.performManualSync() },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // New section: What to sync
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "What to sync",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // History sync toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "History",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Switch(
                                    checked = syncHistory,
                                    onCheckedChange = {
                                        viewModel.updateSyncHistoryEnabled(it)
                                    }
                                )
                            }

                            // Bookmarks sync toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmarks,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Bookmarks",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Switch(
                                    checked = syncBookmarks,
                                    onCheckedChange = {
                                        viewModel.updateSyncBookmarksEnabled(it)
                                    }
                                )
                            }

                            // Open tabs sync toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tab,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Open Tabs",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Switch(
                                    checked = syncTabs,
                                    onCheckedChange = {
                                        viewModel.updateSyncTabsEnabled(it)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Sign out button.
                        if (isLoading) {
                            OutlinedButton(
                                onClick = { /* no action while loading */ },
                                modifier = Modifier.align(Alignment.End),
                                enabled = false
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Signing Out...")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showSignOutDialog = true },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(
                                    Icons.Default.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign Out")
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable {
                            viewModel.getAccessToken { token ->
                                val dashboardUrl =
                                    "https://nimbus-browser-backend-production.up.railway.app/dashboard?token=$token&mobile=true"
                                onNavigateToUrl(dashboardUrl)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .weight(1f)
                            ) {
                                Text(
                                    text = "Manage Your Account",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Access your account dashboard",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onNavigateToUrl("https://nimbus-browser-backend-production.up.railway.app/?mobile=true")
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(
                                modifier = Modifier
                                    .padding(start = 16.dp)
                                    .weight(1f)
                            ) {
                                Text(
                                    text = "Sign in to sync",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Synchronize tabs, bookmarks, passwords, and more",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // Sign out confirmation dialog.
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out? Your data will remain synced to your account.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        isLoading = true
                        scope.launch {
                            try {
                                viewModel.signOut()
                                onNavigateBack()
                            } catch (e: Exception) {
                                // Handle error.
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
fun SyncSettingItem(
    title: String,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )

        Switch(
            checked = isEnabled,
            onCheckedChange = { /* Update sync setting */ }
        )
    }
}