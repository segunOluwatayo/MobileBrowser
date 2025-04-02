package com.example.mobilebrowser.sync

import com.example.mobilebrowser.api.HistoryApiService
import com.example.mobilebrowser.data.dto.HistoryDto
import com.example.mobilebrowser.data.entity.HistoryEntity
import com.example.mobilebrowser.data.repository.HistoryRepository
import com.example.mobilebrowser.data.util.UserDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The UserSyncManager orchestrates the two-way synchronization between the local database
 * and the remote MongoDB server. It handles:
 * - Initial data fetching after login.
 * - Pushing local changes to the server.
 * - Conflict resolution (last-write-wins based on timestamps).
 * - A retry mechanism for handling network failures.
 */
@Singleton
class UserSyncManager @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val historyApiService: HistoryApiService,
    private val userDataStore: UserDataStore
) {
    /**
     * Performs the initial sync after the user logs in.
     *
     * @param authToken The authentication token for API calls.
     * @param deviceId The device identification header.
     * @param userId The current user's identifier.
     */
    suspend fun initialSync(authToken: String, deviceId: String, userId: String) {
        try {
            // Fetch remote history entries for the user.
            val response = historyApiService.getHistory("Bearer $authToken", deviceId, userId)
            if (response.isSuccessful) {
                val remoteHistoryList = response.body() ?: emptyList()
                // Merge remote history with the local database.
                mergeRemoteHistory(remoteHistoryList)
            } else {
                // Handle error response (logging, notifying the user, etc.)
                // e.g., Log.e("UserSyncManager", "Error fetching remote history: ${response.errorBody()}")
            }
        } catch (e: Exception) {
            // Handle exceptions such as network errors.
            // e.g., Log.e("UserSyncManager", "Exception during initial sync: ${e.message}")
        }
    }

    /**
     * Merges remote history entries with the local database.
     * For each remote entry:
     * - If a corresponding local entry exists, compare the timestamps.
     * - If the remote entry is newer, update the local record.
     * - Otherwise, insert the remote entry as new.
     *
     * @param remoteHistory The list of history entries fetched from the server.
     */
    private suspend fun mergeRemoteHistory(remoteHistory: List<HistoryDto>) {
        for (remoteEntry in remoteHistory) {
            // Retrieve the local entry by URL.
            val localEntry = historyRepository.getHistoryByUrl(remoteEntry.url)
            if (localEntry != null) {
                // Compare timestamps for conflict resolution.
                if (remoteEntry.timestamp.after(localEntry.lastModified)) {
                    // Update local entry with remote data.
                    historyRepository.updateHistoryFromDto(remoteEntry)
                }
            } else {
                // No matching local entry found; insert the remote entry.
                historyRepository.insertHistoryFromDto(remoteEntry)
            }
        }
    }

    /**
     * Pushes local changes (pending uploads) to the remote server.
     * Implements a retry mechanism with exponential backoff for transient errors.
     *
     * @param authToken The authentication token for API calls.
     * @param deviceId The device identification header.
     * @param userId The current user's identifier.
     */
    suspend fun pushLocalChanges(authToken: String, deviceId: String, userId: String) {
        // Collect entries that are pending upload from the local database.
        historyRepository.getPendingUploads().collect { pendingList ->
            for (entry in pendingList) {
                // Convert the local HistoryEntity into the HistoryDto expected by the API.
                val dto = entry.toDto(deviceId)
                var retryCount = 0
                var success = false
                // Retry up to 3 times for failed network calls.
                while (retryCount < 3 && !success) {
                    try {
                        val response = historyApiService.addOrUpdateHistory("Bearer $authToken", deviceId, dto)
                        if (response.isSuccessful) {
                            // Update the local entry's sync status to SYNCED and store the server ID if provided.
                            val updatedId = response.body()?.id
                            historyRepository.markAsSynced(entry, updatedId)
                            success = true
                        } else {
                            retryCount++
                            delay(1000L * retryCount) // Exponential backoff.
                        }
                    } catch (e: Exception) {
                        retryCount++
                        delay(1000L * retryCount)
                    }
                }
            }
        }
    }
}

/**
 * Extension function to convert a HistoryEntity to a HistoryDto.
 * Adapts the local model to match the backend schema.
 *
 * @param deviceId The device identification value to be sent with the request.
 * @return A HistoryDto containing fields required by the backend.
 */
fun HistoryEntity.toDto(deviceId: String): HistoryDto {
    return HistoryDto(
        id = this.serverId, // May be null if not synced yet.
        userId = this.userId,
        url = this.url,
        title = this.title,
        // Using lastModified as the timestamp for synchronization purposes.
        timestamp = this.lastModified,
        device = deviceId
    )
}
