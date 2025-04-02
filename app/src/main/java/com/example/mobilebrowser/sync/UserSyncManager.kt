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
     * Pushes local history changes (both additions and deletions) to the server.
     *
     * @param accessToken The user's authentication token
     * @param deviceId The device identifier
     * @param userId The user's ID
     */
    suspend fun pushLocalChanges(accessToken: String, deviceId: String, userId: String) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Process new/updated entries
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

                        // Mark as synced if successful
                        val serverId = response.data.id
                        historyRepository.markAsSynced(entry, serverId)
                        Log.d(TAG, "Successfully synced history entry: ${entry.url}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error syncing history entry: ${e.message}", e)
                    }
                }

                // 2. Process deleted entries
                val pendingDeletes = historyRepository.getPendingDeletes().first()
                Log.d(TAG, "Found ${pendingDeletes.size} history entries pending deletion")

                for (entry in pendingDeletes) {
                    try {
                        // Skip entries not belonging to the current user
                        if (entry.userId != userId) continue

                        // If the entry has a server ID, delete it from the server
                        if (!entry.serverId.isNullOrBlank()) {
                            historyApiService.deleteHistoryEntry(
                                "Bearer $accessToken",
                                entry.serverId
                            )
                            Log.d(TAG, "Successfully deleted history entry from server: ${entry.url}")
                        } else {
                            // If no server ID, check if we need to delete by URL
                            val dto = entry.toDto(deviceId)
                            historyApiService.deleteHistoryEntryByUrl(
                                "Bearer $accessToken",
                                dto.url
                            )
                            Log.d(TAG, "Successfully deleted history entry by URL: ${entry.url}")
                        }

                        // Permanently remove from local database now that server is updated
                        historyRepository.finalizeDeletion(entry)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting history entry: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during history sync: ${e.message}", e)
                throw e
            }
        }
    }
}