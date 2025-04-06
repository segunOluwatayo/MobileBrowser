package com.example.mobilebrowser.ui.composables

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mobilebrowser.sync.SyncStatusState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SyncStatusIndicator(
    syncStatus: SyncStatusState,
    lastSyncTimestamp: Long?,
    onSyncClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Rotating animation for the sync icon when syncing
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Format the timestamp
    val formattedTime = remember(lastSyncTimestamp) {
        lastSyncTimestamp?.let {
            val date = Date(it)
            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            formatter.format(date)
        } ?: "Never"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side - status and time
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status icon
                    when (syncStatus) {
                        is SyncStatusState.Syncing -> {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Syncing",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.rotate(rotation)
                            )
                            Text(
                                "Syncing...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is SyncStatusState.Synced -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Synced",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Synced",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        is SyncStatusState.Error -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Sync error",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {
                            Text(
                                "Sync Status: Idle",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Last sync: $formattedTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Show error message if there is one
                if (syncStatus is SyncStatusState.Error) {
                    Text(
                        text = syncStatus.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Right side - sync button
            Button(
                onClick = onSyncClicked,
                enabled = syncStatus !is SyncStatusState.Syncing
            ) {
                Text("Sync Now")
            }
        }
    }

    // Auto-sync information
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = "Auto-sync: Every 3 minutes when signed in",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}