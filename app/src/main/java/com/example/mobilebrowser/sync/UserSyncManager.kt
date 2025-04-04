package com.example.mobilebrowser.sync

import android.util.Log
import com.example.mobilebrowser.api.HistoryApiService
import com.example.mobilebrowser.data.dto.ApiResponse
import com.example.mobilebrowser.data.dto.HistoryDto
import com.example.mobilebrowser.data.entity.SyncStatus
import com.example.mobilebrowser.data.repository.HistoryRepository
import com.example.mobilebrowser.data.repository.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncStatusState {
    object Idle : SyncStatusState()
    object Syncing : SyncStatusState()
    object Synced : SyncStatusState()
    data class Error(val message: String) : SyncStatusState()
}

/**
 * Enhanced UserSyncManager to handle both uploads and deletions.
 * Now properly syncs deleted history entries to the server.
 */
@Singleton
class UserSyncManager @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val historyApiService: HistoryApiService
) {
    private val TAG = "UserSyncManager"

    /**
     * Directly deletes a history entry from the server using its server ID
     *
     * @param historyId The server ID of the history entry to delete
     * @param accessToken The user's access token for authentication
     */
    suspend fun deleteHistoryEntryFromServer(historyId: String, accessToken: String) {
        try {
            // Call the API to delete the entry directly
            historyApiService.deleteHistoryEntry("Bearer $accessToken", historyId)
        } catch (e: Exception) {
            throw Exception("Failed to delete history from server: ${e.message}")
        }
    }

    /**
     * Process pending history deletions during sync
     *
     * @param accessToken User's authentication token
     * @param deviceId Current device identifier
     * @param userId User identifier
     * @return Number of successfully processed deletions
     */
    private suspend fun processPendingHistoryDeletions(
        accessToken: String,
        deviceId: String,
        userId: String
    ): Int {
        var deletionCount = 0

        try {
            // Get all history entries
            val allHistory = historyRepository.getAllHistoryAsList()

            // Filter for entries with the PENDING_DELETE: URL prefix
            val pendingDeletions = allHistory.filter { it.url.startsWith("PENDING_DELETE:") }

            for (pendingDelete in pendingDeletions) {
                try {
                    // Extract the original URL from the pending delete entry
                    val originalUrl = pendingDelete.url.removePrefix("PENDING_DELETE:")

                    // If the entry has a server ID, use that for deletion
                    if (!pendingDelete.serverId.isNullOrBlank()) {
                        historyApiService.deleteHistoryEntry(
                            "Bearer $accessToken",
                            pendingDelete.serverId
                        )
                    } else {
                        // Otherwise try to find and delete by URL
                        try {
                            // Find the item on the server by URL and delete it
                            historyApiService.deleteHistoryEntryByUrl(
                                "Bearer $accessToken",
                                originalUrl,
                                userId,
                                deviceId
                            )
                        } catch (e: Exception) {
                            // If we couldn't find by URL, that's OK - it might not exist on server
                            // Just log it and continue
                            Log.d("UserSyncManager", "Could not find history item to delete: $originalUrl")
                        }
                    }

                    // Remove the pending deletion entry after we've processed it
                    historyRepository.finalizeDeletion(pendingDelete)
                    deletionCount++

                } catch (e: Exception) {
                    Log.e("UserSyncManager", "Error processing deletion for ${pendingDelete.url}: ${e.message}")
                    // We'll leave the pending deletion entry for the next sync attempt
                }
            }
        } catch (e: Exception) {
            Log.e("UserSyncManager", "Error during pending deletion processing: ${e.message}")
        }

        return deletionCount
    }

    /**
     * Pushes local history changes (both additions and deletions) to the server.
     *
     * @param accessToken The user's authentication token
     * @param deviceId The device identifier
     * @param userId The user's ID
     */
    suspend fun pushLocalChanges(accessToken: String, deviceId: String, userId: String) {
        val deletionsProcessed = processPendingHistoryDeletions(accessToken, deviceId, userId)
        Log.d(TAG, "Processed $deletionsProcessed history items with PENDING_DELETE URL prefix")
        withContext(Dispatchers.IO) {
            try {
                // 1. Handle pending deletions (enhanced logic)
                try {
                    val pendingDeletes = historyRepository.getAllHistoryAsList()
                        .filter {
                            it.syncStatus == SyncStatus.PENDING_DELETE &&
                                    !it.url.startsWith("PENDING_DELETE:") // Skip items already processed
                        }


                    Log.d(TAG, "Found ${pendingDeletes.size} history entries pending deletion (enhanced)")

                    for (deleteItem in pendingDeletes) {
                        try {
                            // Skip entries not belonging to the current user
                            if (deleteItem.userId != userId) continue

                            if (!deleteItem.serverId.isNullOrBlank()) {
                                historyApiService.deleteHistoryEntry(
                                    "Bearer $accessToken",
                                    deleteItem.serverId
                                )
                                Log.d(TAG, "Successfully deleted history entry by server ID: ${deleteItem.url}")
                            } else {
                                val originalUrl = if (deleteItem.url.startsWith("PENDING_DELETE:")) {
                                    deleteItem.url.removePrefix("PENDING_DELETE:")
                                } else {
                                    deleteItem.url
                                }

                                historyApiService.deleteHistoryEntryByUrl(
                                    "Bearer $accessToken",
                                    originalUrl,
                                    userId,
                                    deviceId
                                )
                                Log.d(TAG, "Successfully deleted history entry by URL: $originalUrl")
                            }

                            historyRepository.finalizeDeletion(deleteItem)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error deleting item from server: ${e.message}", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing pending deletions: ${e.message}", e)
                }

                // 2. Handle pending uploads (unchanged logic)
                val pendingUploads = historyRepository.getPendingUploads().first()
                Log.d(TAG, "Found ${pendingUploads.size} history entries pending upload")

                for (entry in pendingUploads) {
                    try {
                        // Skip entries not belonging to the current user
                        if (entry.userId != userId) continue

                        val dto = entry.toDto(deviceId)
                        val response = historyApiService.addHistoryEntry(
                            "Bearer $accessToken",
                            dto
                        )

                        val serverId = response.data.id
                        historyRepository.markAsSynced(entry, serverId)
                        Log.d(TAG, "Successfully synced history entry: ${entry.url}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing history entry: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during history sync: ${e.message}", e)
                throw e
            }
        }
    }
    private suspend fun syncPendingDeletes(accessToken: String, deviceId: String) {
        // Get all entries marked for deletion
        val pendingDeletes = historyRepository.getPendingDeletes().first()

        for (entry in pendingDeletes) {
            if (entry.url.startsWith("PENDING_DELETE:")) {
                // Extract the real URL
                val realUrl = entry.url.substringAfter("PENDING_DELETE:")

                try {
                    // Try to delete on server by URL if we have a special endpoint
                    // historyApiService.deleteHistoryEntryByUrl("Bearer $accessToken", realUrl)

                    // Remove the shadow entry after server deletion
                    historyRepository.finalizeDeletion(entry)
                } catch (e: Exception) {
                    // Log error, keep shadow entry for next sync attempt
                    Log.e("UserSyncManager", "Failed to sync delete: ${e.message}")
                }
            }
        }
    }
}