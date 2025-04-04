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

            Log.d(TAG, "Auth credentials obtained. UserID: $userId")

            // Push pending changes for history
            pushPendingHistory(accessToken, deviceId, userId)

            // Pull remote history data
            pullRemoteHistory(accessToken)

            // Push pending changes for bookmarks
            pushPendingBookmarks(accessToken, deviceId, userId)

            // Pull remote bookmark data
            pullRemoteBookmarks(accessToken)

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
    private suspend fun pullRemoteBookmarks(token: String) {
        try {
            Log.d(TAG, "Pulling remote bookmarks from server")
            val response: ApiResponse<List<BookmarkDto>> = bookmarkApiService.getAllBookmarks("Bearer $token")
            val remoteEntries = response.data
            Log.d(TAG, "Received ${remoteEntries.size} bookmarks from server")
            remoteEntries.forEach { remoteEntry ->
                try {
                    val localEntry = bookmarkRepository.getBookmarkByUrl(remoteEntry.url)
                    if (localEntry == null) {
                        val newEntry = com.example.mobilebrowser.data.entity.BookmarkEntity(
                            title = remoteEntry.title,
                            url = remoteEntry.url,
                            userId = remoteEntry.userId,
                            favicon = remoteEntry.favicon,
                            lastVisited = remoteEntry.timestamp,
                            tags = remoteEntry.tags,
                            dateAdded = remoteEntry.timestamp,
                            serverId = remoteEntry.id,
                            syncStatus = SyncStatus.SYNCED
                        )
                        bookmarkRepository.addBookmark(newEntry)
                        Log.d(TAG, "Inserted new bookmark from server: ${remoteEntry.url}")
                    } else if (localEntry.syncStatus != SyncStatus.PENDING_UPLOAD) {
                        if (remoteEntry.timestamp.after(localEntry.dateAdded)) {
                            val updatedEntry = localEntry.copy(
                                title = remoteEntry.title,
                                url = remoteEntry.url,
                                favicon = remoteEntry.favicon,
                                lastVisited = remoteEntry.timestamp,
                                tags = remoteEntry.tags,
                                dateAdded = remoteEntry.timestamp,
                                serverId = remoteEntry.id,
                                syncStatus = SyncStatus.SYNCED
                            )
                            bookmarkRepository.updateBookmark(updatedEntry)
                            Log.d(TAG, "Updated local bookmark from server: ${remoteEntry.url}")
                        }
                    } else {
                        Log.d(TAG, "Local bookmark ${remoteEntry.url} has pending changes. Skipping update.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing remote bookmark ${remoteEntry.url}: ${e.message}")
                }
            }
            Log.d(TAG, "Completed pulling remote bookmarks")
        } catch (e: Exception) {
            Log.e(TAG, "Error pulling remote bookmarks: ${e.message}", e)
            throw e
        }
    }
}
