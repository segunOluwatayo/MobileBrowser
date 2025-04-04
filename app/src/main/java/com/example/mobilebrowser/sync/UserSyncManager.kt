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

@Singleton
class UserSyncManager @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val historyApiService: HistoryApiService
) {
    private val TAG = "UserSyncManager"

    /**
     * Deletes a history entry from the server by its server ID.
     * Throws an Exception if it fails (so you can catch/handle it).
     */
    suspend fun deleteHistoryEntryFromServer(historyId: String, accessToken: String) {
        try {
            historyApiService.deleteHistoryEntry("Bearer $accessToken", historyId)
        } catch (e: Exception) {
            throw Exception("Failed to delete history from server: ${e.message}")
        }
    }

    /**
     * Pushes all local history changes (pending deletions and pending uploads) to the server.
     *
     * 1) Pending Deletions:
     *    - If serverId exists, delete by serverId.
     *    - Otherwise try to delete by URL (removing "PENDING_DELETE:" prefix if present).
     *    - If successful (or 404), remove locally so it won't return on next sync.
     *
     * 2) Pending Uploads:
     *    - POST each item to the server, then mark as SYNCED with the new serverId.
     */
    suspend fun pushLocalChanges(accessToken: String, deviceId: String, userId: String) {
        withContext(Dispatchers.IO) {
            /** -- 1) Handle PENDING_DELETE items -- **/
            try {
                // Grab every entry that has syncStatus == PENDING_DELETE:
                val pendingDeletes = historyRepository.getAllHistoryAsList()
                    .filter { it.syncStatus == SyncStatus.PENDING_DELETE }

                Log.d(TAG, "Found ${pendingDeletes.size} history entries pending deletion")

                for (deleteItem in pendingDeletes) {
                    // Only delete items that actually belong to this user
                    if (deleteItem.userId != userId) continue

                    try {
                        if (!deleteItem.serverId.isNullOrBlank()) {
                            // If we have a server ID, delete by ID
                            historyApiService.deleteHistoryEntry("Bearer $accessToken", deleteItem.serverId)
                            Log.d(TAG, "Deleted item from server by ID: ${deleteItem.url}")
                        } else {
                            // Otherwise, delete by URL (remove "PENDING_DELETE:" prefix if present)
                            val originalUrl = if (deleteItem.url.startsWith("PENDING_DELETE:")) {
                                deleteItem.url.removePrefix("PENDING_DELETE:")
                            } else {
                                deleteItem.url
                            }

                            // Attempt delete by URL
                            try {
                                historyApiService.deleteHistoryEntryByUrl("Bearer $accessToken", originalUrl)
                                Log.d(TAG, "Deleted item from server by URL: $originalUrl")
                            } catch (notFound: Exception) {
                                // If 404 or other error, just log & assume server doesn't have it
                                Log.d(TAG, "Could not find item on server for url: $originalUrl. Ignoring.")
                            }
                        }

                        // Once done (success or 404), finalize local deletion to remove it permanently
                        historyRepository.finalizeDeletion(deleteItem)

                    } catch (deleteError: Exception) {
                        // If we fail, we keep the item in pending-delete so next sync tries again
                        Log.e(TAG, "Error deleting item ${deleteItem.url} from server: ${deleteError.message}", deleteError)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling pending deletions: ${e.message}", e)
            }

            /** -- 2) Handle PENDING_UPLOAD items -- **/
            try {
                val pendingUploads = historyRepository.getPendingUploads().first()
                Log.d(TAG, "Found ${pendingUploads.size} history entries pending upload")

                for (entry in pendingUploads) {
                    // Only sync items that belong to this user
                    if (entry.userId != userId) continue

                    try {
                        // Convert local entity to DTO
                        val dto = entry.toDto(deviceId)
                        val response = historyApiService.addHistoryEntry("Bearer $accessToken", dto)
                        val serverId = response.data.id

                        // Mark local entry as SYNCED with the new server ID
                        historyRepository.markAsSynced(entry, serverId)
                        Log.d(TAG, "Successfully uploaded: ${entry.url}")
                    } catch (uploadError: Exception) {
                        // Keep in PENDING_UPLOAD so we can retry next time
                        Log.e(TAG, "Error uploading entry ${entry.url}: ${uploadError.message}", uploadError)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling pending uploads: ${e.message}", e)
                throw e // Rethrow if you want the caller to see it
            }
        }
    }
}
