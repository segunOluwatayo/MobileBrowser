package com.example.mobilebrowser.sync

import android.util.Log
import com.example.mobilebrowser.api.HistoryApiService
import com.example.mobilebrowser.api.BookmarkApiService
import com.example.mobilebrowser.data.dto.ApiResponse
import com.example.mobilebrowser.data.dto.HistoryDto
import com.example.mobilebrowser.data.dto.BookmarkDto
import com.example.mobilebrowser.data.entity.SyncStatus
import com.example.mobilebrowser.data.repository.HistoryRepository
import com.example.mobilebrowser.data.repository.BookmarkRepository
import com.example.mobilebrowser.data.repository.toDto
import com.example.mobilebrowser.data.util.UserDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InitialSyncManager @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val historyApiService: HistoryApiService,
    private val bookmarkRepository: BookmarkRepository,
    private val bookmarkApiService: BookmarkApiService,
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

            Log.d(TAG, "Auth credentials obtained. UserID: $userId, DeviceID: $deviceId")

            try {
                // First try to pull remote data
                Log.d(TAG, "Starting with pull operations to populate local database")

                // Pull remote bookmark data first
                Log.d(TAG, "Pulling remote bookmarks first...")
                pullRemoteBookmarks(accessToken)

                // Pull remote history data
                Log.d(TAG, "Pulling remote history...")
                pullRemoteHistory(accessToken)

                // Now push any pending changes
                Log.d(TAG, "Now pushing any pending local changes")

                // Push pending changes for bookmarks
                Log.d(TAG, "Pushing pending bookmarks...")
                pushPendingBookmarks(accessToken, deviceId, userId)

                // Push pending changes for history
                Log.d(TAG, "Pushing pending history...")
                pushPendingHistory(accessToken, deviceId, userId)

                Log.d(TAG, "Initial sync completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during specific sync operation: ${e.message}", e)

                // Try each operation individually to maximize chances of partial success
                try {
                    pullRemoteBookmarks(accessToken)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to pull remote bookmarks: ${e.message}", e)
                }

                try {
                    pullRemoteHistory(accessToken)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to pull remote history: ${e.message}", e)
                }

                throw e  // Re-throw the original exception
            }
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
            val pendingEntries = historyRepository.getPendingUploads().first()
            Log.d(TAG, "Found ${pendingEntries.size} pending history entries to upload")
            if (pendingEntries.isEmpty()) {
                Log.d(TAG, "No pending history entries to push")
                return
            }
            pendingEntries.forEach { entry ->
                try {
                    val dto = entry.toDto(deviceId)
                    Log.d(TAG, "Pushing history entry: $dto")
                    val response: ApiResponse<HistoryDto> = historyApiService.addHistoryEntry("Bearer $token", dto)
                    val serverId = response.data.id
                    historyRepository.markAsSynced(entry, serverId)
                    Log.d(TAG, "Successfully synced history entry: ${entry.url}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync history entry ${entry.url}: ${e.message}")
                }
            }
            Log.d(TAG, "Completed pushing pending history entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing pending history: ${e.message}", e)
            throw e
        }
    }

    /**
     * Pulls history entries from the remote server and updates the local database.
     */
    private suspend fun pullRemoteHistory(token: String) {
        try {
            Log.d(TAG, "Pulling remote history from server")
            val response: ApiResponse<List<HistoryDto>> = historyApiService.getHistory("Bearer $token")
            val remoteEntries = response.data
            Log.d(TAG, "Received ${remoteEntries.size} history entries from server")
            remoteEntries.forEach { remoteEntry ->
                try {
                    val localEntry = historyRepository.getHistoryByUrl(remoteEntry.url)
                    if (localEntry == null) {
                        historyRepository.insertHistoryFromDto(remoteEntry)
                        Log.d(TAG, "Added new history entry from server: ${remoteEntry.url}")
                    } else if (localEntry.syncStatus != SyncStatus.PENDING_UPLOAD) {
                        historyRepository.updateHistoryFromDto(remoteEntry)
                        Log.d(TAG, "Updated local history entry from server: ${remoteEntry.url}")
                    } else {
                        Log.d(TAG, "Keeping local version of ${remoteEntry.url} due to pending changes")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing remote history entry ${remoteEntry.url}: ${e.message}")
                }
            }
            Log.d(TAG, "Completed pulling remote history")
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling remote history: ${e.message}", e)
            throw e
        }
    }

    /**
     * Pushes all local bookmark entries marked as PENDING_UPLOAD to the server.
     */
    private suspend fun pushPendingBookmarks(token: String, deviceId: String, userId: String) {
        try {
            Log.d(TAG, "Pushing pending bookmarks to server")
            val pendingEntries = bookmarkRepository.getPendingUploads()
            Log.d(TAG, "Found ${pendingEntries.size} pending bookmark entries to upload")
            if (pendingEntries.isEmpty()) {
                Log.d(TAG, "No pending bookmark entries to push")
                return
            }
            pendingEntries.forEach { entry ->
                try {
                    val dto = entry.toDto(userId)
                    Log.d(TAG, "Pushing bookmark entry: $dto")
                    val response: ApiResponse<BookmarkDto> = bookmarkApiService.addBookmark("Bearer $token", dto)
                    val serverId = response.data.id
                    bookmarkRepository.markAsSynced(entry, serverId)
                    Log.d(TAG, "Successfully synced bookmark entry: ${entry.url}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync bookmark entry ${entry.url}: ${e.message}")
                }
            }
            Log.d(TAG, "Completed pushing pending bookmark entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing pending bookmarks: ${e.message}", e)
            throw e
        }
    }

    /**
     * Pulls bookmark entries from the remote server and updates the local database.
     */
    // Update this method in your InitialSyncManager.kt file

    private suspend fun pullRemoteBookmarks(token: String) {
        try {
            Log.d(TAG, "Pulling remote bookmarks from server")
            val response: ApiResponse<List<BookmarkDto>> = bookmarkApiService.getAllBookmarks("Bearer $token")
            val remoteEntries = response.data
            Log.d(TAG, "Received ${remoteEntries.size} bookmarks from server")

            // Get current userId for verification
            val currentUserId = userDataStore.userId.first()
            Log.d(TAG, "Current user ID: $currentUserId")

            remoteEntries.forEach { remoteEntry ->
                try {
                    // Add logging for each bookmark being processed
                    Log.d(TAG, "Processing remote bookmark: ${remoteEntry.url}, userId: ${remoteEntry.userId}, serverId: ${remoteEntry.id}")

                    // Create default values for any potentially null fields
                    val currentTime = Date()

                    val localEntry = bookmarkRepository.getBookmarkByUrl(remoteEntry.url)
                    if (localEntry == null) {
                        // Create a new bookmark entity with the CORRECT userId from the remote entry
                        // and handle null timestamp safely
                        val newEntry = com.example.mobilebrowser.data.entity.BookmarkEntity(
                            title = remoteEntry.title ?: "Untitled",
                            url = remoteEntry.url,
                            userId = remoteEntry.userId ?: currentUserId, // Fallback to current user ID if server's is null
                            favicon = remoteEntry.favicon,
                            // Use server timestamp if available, otherwise use current time
                            lastVisited = remoteEntry.timestamp ?: currentTime,
                            tags = remoteEntry.tags,
                            // Use server timestamp if available, otherwise use current time
                            dateAdded = remoteEntry.timestamp ?: currentTime,
                            serverId = remoteEntry.id,
                            syncStatus = SyncStatus.SYNCED
                        )
                        val bookmarkId = bookmarkRepository.addBookmark(newEntry)
                        Log.d(TAG, "Inserted new bookmark from server: ${remoteEntry.url} with local ID: $bookmarkId")
                    } else if (localEntry.syncStatus != SyncStatus.PENDING_UPLOAD) {
                        // Only update if the remote entry is newer or if we have no timestamp to compare
                        val shouldUpdate = if (remoteEntry.timestamp != null && localEntry.dateAdded != null) {
                            remoteEntry.timestamp.after(localEntry.dateAdded)
                        } else {
                            // If timestamps can't be compared, just update
                            true
                        }

                        if (shouldUpdate) {
                            val updatedEntry = localEntry.copy(
                                title = remoteEntry.title ?: localEntry.title,
                                url = remoteEntry.url,
                                // Make sure to preserve the userId from the remote entry
                                userId = remoteEntry.userId ?: localEntry.userId,
                                favicon = remoteEntry.favicon ?: localEntry.favicon,
                                // Keep existing lastVisited if remote is null
                                lastVisited = remoteEntry.timestamp ?: localEntry.lastVisited,
                                tags = remoteEntry.tags ?: localEntry.tags,
                                // Keep existing dateAdded if remote is null
                                dateAdded = remoteEntry.timestamp ?: localEntry.dateAdded,
                                serverId = remoteEntry.id ?: localEntry.serverId,
                                syncStatus = SyncStatus.SYNCED
                            )
                            bookmarkRepository.updateBookmark(updatedEntry)
                            Log.d(TAG, "Updated local bookmark from server: ${remoteEntry.url}")
                        } else {
                            Log.d(TAG, "Remote bookmark not newer than local, skipping update")
                        }
                    } else {
                        Log.d(TAG, "Local bookmark ${remoteEntry.url} has pending changes. Skipping update.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing remote bookmark ${remoteEntry.url}: ${e.message}", e)
                    // Log detailed error information to help diagnose issues
                    e.printStackTrace()
                }
            }
            Log.d(TAG, "Completed pulling remote bookmarks")
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling remote bookmarks: ${e.message}", e)
            e.printStackTrace()
            throw e
        }
    }
}
