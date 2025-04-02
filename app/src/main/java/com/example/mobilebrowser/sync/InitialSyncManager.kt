package com.example.mobilebrowser.sync

import android.util.Log
import com.example.mobilebrowser.api.HistoryApiService
import com.example.mobilebrowser.data.entity.HistoryEntity
import com.example.mobilebrowser.data.entity.SyncStatus
import com.example.mobilebrowser.data.repository.HistoryRepository
import com.example.mobilebrowser.data.util.UserDataStore
import com.example.mobilebrowser.data.repository.toDto
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the initial synchronization of data after user authentication.
 * Handles both pulling remote data and pushing pending local changes.
 */
@Singleton
class InitialSyncManager @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val historyApiService: HistoryApiService,
    private val userDataStore: UserDataStore
) {
    private val TAG = "InitialSyncManager"

    /**
     * Performs a complete initial synchronization of all data types.
     * This should be called after successful authentication.
     */
    suspend fun performInitialSync() {
        try {
            Log.d(TAG, "Starting initial sync process")

            // Get authentication credentials
            val accessToken = userDataStore.accessToken.first()
            val userId = userDataStore.userId.first()
            val deviceId = userDataStore.deviceId.first().ifEmpty { "android-device" }

            if (accessToken.isBlank() || userId.isBlank()) {
                Log.e(TAG, "Cannot sync: Missing auth credentials. Token: ${accessToken.isNotBlank()}, UserID: ${userId.isNotBlank()}")
                throw IllegalStateException("Authentication credentials not available for sync")
            }

            Log.d(TAG, "Auth credentials obtained. UserID: $userId")

            // Push all pending changes first
            pushPendingHistory(accessToken, deviceId, userId)

            // Then pull remote data
            pullRemoteHistory(accessToken)

            Log.d(TAG, "Initial sync completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Initial sync failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Pushes all local history entries marked as PENDING_UPLOAD to the server.
     */
    private suspend fun pushPendingHistory(token: String, deviceId: String, userId: String) {
        try {
            Log.d(TAG, "Pushing pending history to server")

            // Get all pending history entries
            val pendingEntries = historyRepository.getPendingUploads().first()
            Log.d(TAG, "Found ${pendingEntries.size} pending history entries to upload")

            if (pendingEntries.isEmpty()) {
                Log.d(TAG, "No pending history entries to push")
                return
            }

            // Process each pending entry
            pendingEntries.forEach { entry ->
                try {
                    // Create a DTO for this entry
                    val dto = entry.toDto(deviceId)

                    Log.d(TAG, "Pushing history entry: $dto")

                    // Make API call to sync this entry
                    val response = historyApiService.addHistoryEntry("Bearer $token", dto)

                    // Update the local entry to mark it as synced
                    val serverId = response.data.id
                    historyRepository.markAsSynced(entry, serverId)

                    Log.d(TAG, "Successfully synced history entry: ${entry.url}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync history entry ${entry.url}: ${e.message}")
                    // Continue with other entries even if one fails
                }
            }

            Log.d(TAG, "Completed pushing pending history entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing pending history: ${e.message}", e)
            throw e
        }
    }

    /**
     * Pulls history entries from the remote server and updates local database.
     */
    private suspend fun pullRemoteHistory(token: String) {
        try {
            Log.d(TAG, "Pulling remote history from server")

            // Get remote history entries
            val response = historyApiService.getHistory("Bearer $token")
            val remoteEntries = response.data

            Log.d(TAG, "Received ${remoteEntries.size} history entries from server")

            // Process each remote entry
            remoteEntries.forEach { remoteEntry ->
                try {
                    // Check if we already have this entry locally by URL
                    val localEntry = historyRepository.getHistoryByUrl(remoteEntry.url)

                    if (localEntry == null) {
                        // New entry from server, add it locally
                        historyRepository.insertHistoryFromDto(remoteEntry)
                        Log.d(TAG, "Added new history entry from server: ${remoteEntry.url}")
                    } else if (localEntry.syncStatus != SyncStatus.PENDING_UPLOAD) {
                        // Local entry exists but no pending changes, update with server data
                        historyRepository.updateHistoryFromDto(remoteEntry)
                        Log.d(TAG, "Updated local history entry from server: ${remoteEntry.url}")
                    } else {
                        // Local has pending changes, keep local version for now
                        Log.d(TAG, "Keeping local version of ${remoteEntry.url} due to pending changes")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing remote history entry ${remoteEntry.url}: ${e.message}")
                    // Continue with other entries even if one fails
                }
            }

            Log.d(TAG, "Completed pulling remote history")
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling remote history: ${e.message}", e)
            throw e
        }
    }
}