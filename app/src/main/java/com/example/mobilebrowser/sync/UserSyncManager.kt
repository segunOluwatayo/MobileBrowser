package com.example.mobilebrowser.sync

import android.util.Log
import com.example.mobilebrowser.api.HistoryApiService
import com.example.mobilebrowser.data.entity.SyncStatus
import com.example.mobilebrowser.data.repository.HistoryRepository
import com.example.mobilebrowser.data.repository.toDto
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * State class for tracking sync operation status.
 */
sealed class SyncStatusState {
    object Idle : SyncStatusState()
    object Syncing : SyncStatusState()
    object Synced : SyncStatusState()
    data class Error(val message: String) : SyncStatusState()
}

/**
 * Manages synchronization operations for user data.
 * Handles pushing local changes to server and pulling remote changes.
 */
@Singleton
class UserSyncManager @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val historyApiService: HistoryApiService
) {
    private val TAG = "UserSyncManager"

    /**
     * Pushes local pending changes to the server.
     * This includes:
     * - New history entries (PENDING_UPLOAD)
     * - Deleted history entries (PENDING_DELETE)
     *
     * @param authToken JWT auth token for API calls
     * @param deviceId Device identifier for tracking sync origin
     * @param userId User identifier
     */
    suspend fun pushLocalChanges(authToken: String, deviceId: String, userId: String) {
        try {
            Log.d(TAG, "Starting to push local changes to server")

            // Process pending history uploads
            val pendingUploads = historyRepository.getPendingUploads().first()
            Log.d(TAG, "Found ${pendingUploads.size} pending history uploads")

            for (entry in pendingUploads) {
                try {
                    when (entry.syncStatus) {
                        SyncStatus.PENDING_UPLOAD -> {
                            // Convert to DTO and send to server
                            val dto = entry.toDto(deviceId)

                            // Debug: Log what we're about to send
                            Log.d(TAG, "Syncing history entry: URL=${entry.url}, Title=${entry.title}, UserID=${entry.userId}")

                            val response = historyApiService.addHistoryEntry("Bearer $authToken", dto)

                            // Update local entry with server ID and mark as synced
                            historyRepository.markAsSynced(entry, response.data.id)
                            Log.d(TAG, "Successfully synced history entry: ${entry.url}")
                        }

                        SyncStatus.PENDING_DELETE -> {
                            // Only try to delete from server if we have a server ID
                            if (entry.serverId != null) {
                                historyApiService.deleteHistoryEntry("Bearer $authToken", entry.serverId)

                                // After confirmed deletion on server, delete locally
                                historyRepository.finalizeDeletion(entry)
                                Log.d(TAG, "Successfully deleted history entry from server: ${entry.url}")
                            } else {
                                // If no server ID, just delete locally (was never synced)
                                historyRepository.finalizeDeletion(entry)
                                Log.d(TAG, "Deleted local-only history entry: ${entry.url}")
                            }
                        }

                        else -> {
                            // Skip entries already synced or with conflicts
                            Log.d(TAG, "Skipping history entry with status ${entry.syncStatus}: ${entry.url}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing history entry ${entry.url}: ${e.message}", e)
                    // Continue with other entries
                }
            }

            Log.d(TAG, "Completed pushing local changes")
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing local changes: ${e.message}", e)
            throw e
        }
    }
}