package com.example.mobilebrowser.sync

import android.util.Log
import com.example.mobilebrowser.api.HistoryApiService
import com.example.mobilebrowser.api.BookmarkApiService
import com.example.mobilebrowser.api.TabApiService
import com.example.mobilebrowser.data.dto.ApiResponse
import com.example.mobilebrowser.data.dto.HistoryDto
import com.example.mobilebrowser.data.dto.BookmarkDto
import com.example.mobilebrowser.data.entity.SyncStatus
import com.example.mobilebrowser.data.repository.HistoryRepository
import com.example.mobilebrowser.data.repository.BookmarkRepository
import com.example.mobilebrowser.data.repository.TabRepository
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
    private val tabRepository: TabRepository,
    private val tabApiService: TabApiService,
    private val tabDao: com.example.mobilebrowser.data.dao.TabDao,
    private val userDataStore: UserDataStore
) {
    private val TAG = "InitialSyncManager"

    /**
     * Performs a complete initial synchronization of all data types based on sync preferences.
     * This should be called after successful authentication.
     *
     * @param syncHistory Whether to sync browser history
     * @param syncBookmarks Whether to sync bookmarks
     * @param syncTabs Whether to sync open tabs
     */
    suspend fun performInitialSync(
        syncHistory: Boolean = true,
        syncBookmarks: Boolean = true,
        syncTabs: Boolean = true
    ) {
        try {
            Log.d(TAG, "Starting initial sync process with preferences - " +
                    "History: $syncHistory, Bookmarks: $syncBookmarks, Tabs: $syncTabs")

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
                // First try to pull remote data based on preferences
                Log.d(TAG, "Starting with pull operations to populate local database")

                // Pull remote bookmark data if enabled
                if (syncBookmarks) {
                    Log.d(TAG, "Pulling remote bookmarks...")
                    pullRemoteBookmarks(accessToken)
                } else {
                    Log.d(TAG, "Skipping bookmark pull (disabled in preferences)")
                }

                // Pull remote history data if enabled
                if (syncHistory) {
                    Log.d(TAG, "Pulling remote history...")
                    pullRemoteHistory(accessToken)
                } else {
                    Log.d(TAG, "Skipping history pull (disabled in preferences)")
                }

                // Pull tabs if enabled
                if (syncTabs) {
                    Log.d(TAG, "Pulling remote tabs...")
                    pullRemoteTabs(accessToken, userId)
                } else {
                    Log.d(TAG, "Skipping tabs pull (disabled in preferences)")
                }

                // Now push any pending changes based on preferences
                Log.d(TAG, "Now pushing local changes based on sync preferences")

                // Push tabs if enabled
                if (syncTabs) {
                    Log.d(TAG, "Pushing pending tabs...")
                    pushPendingTabs(accessToken, deviceId, userId)
                } else {
                    Log.d(TAG, "Skipping tabs push (disabled in preferences)")
                }

                // Push pending changes for bookmarks if enabled
                if (syncBookmarks) {
                    Log.d(TAG, "Pushing pending bookmarks...")
                    pushPendingBookmarks(accessToken, deviceId, userId)
                } else {
                    Log.d(TAG, "Skipping bookmarks push (disabled in preferences)")
                }

                // Push pending changes for history if enabled
                if (syncHistory) {
                    Log.d(TAG, "Pushing pending history...")
                    pushPendingHistory(accessToken, deviceId, userId)
                } else {
                    Log.d(TAG, "Skipping history push (disabled in preferences)")
                }

                Log.d(TAG, "Initial sync completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during specific sync operation: ${e.message}", e)

                // Try each enabled operation individually to maximize chances of partial success
                try {
                    if (syncBookmarks) {
                        pullRemoteBookmarks(accessToken)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to pull remote bookmarks: ${e.message}", e)
                }

                try {
                    if (syncHistory) {
                        pullRemoteHistory(accessToken)
                    }
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
     * Overloaded method that reads sync preferences from UserDataStore.
     * This provides backward compatibility with existing code.
     */
    suspend fun performInitialSync() {
        // Read sync preferences from UserDataStore
        val syncHistory = userDataStore.syncHistoryEnabled.first()
        val syncBookmarks = userDataStore.syncBookmarksEnabled.first()
        val syncTabs = userDataStore.syncTabsEnabled.first()

        // Call the main implementation with these preferences
        performInitialSync(
            syncHistory = syncHistory,
            syncBookmarks = syncBookmarks,
            syncTabs = syncTabs
        )
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

            // Get current userId for verification
            val currentUserId = userDataStore.userId.first()
            Log.d(TAG, "Current user ID: $currentUserId")

            // First, get all shadow entries (bookmarks pending deletion)
            // We'll use this to avoid re-adding bookmarks that were intentionally deleted
            val pendingDeletions = bookmarkRepository.getPendingDeletions()
            val pendingDeletionUrls = pendingDeletions.map {
                // Extract the original URL by removing the "PENDING_DELETE:" prefix
                if (it.url.startsWith("PENDING_DELETE:")) {
                    it.url.removePrefix("PENDING_DELETE:")
                } else {
                    it.url
                }
            }

            Log.d(TAG, "Found ${pendingDeletionUrls.size} bookmarks pending deletion")

            remoteEntries.forEach { remoteEntry ->
                try {
                    // Add logging for each bookmark being processed
                    Log.d(TAG, "Processing remote bookmark: ${remoteEntry.url}, userId: ${remoteEntry.userId}, serverId: ${remoteEntry.id}")

                    // Skip bookmarks that are pending deletion locally
                    if (pendingDeletionUrls.contains(remoteEntry.url)) {
                        Log.d(TAG, "Skipping bookmark ${remoteEntry.url} as it's pending deletion locally")
                        return@forEach
                    }

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

    /**
     * Pushes pending tab changes to the server.
     */
    private suspend fun pushPendingTabs(token: String, deviceId: String, userId: String) {
        try {
            Log.d(TAG, "Pushing pending tabs to server")
            val pendingDeletes = tabRepository.getPendingDeletes()
            Log.d(TAG, "Found ${pendingDeletes.size} tabs pending deletion")

            val allPendingUploads = tabRepository.getPendingUploads()
            val pendingUploads = allPendingUploads.filter {
                it.url.isNotBlank() &&
                        it.title != "Loading..."
            }

            Log.d(TAG, "Found ${pendingUploads.size} tabs pending upload (filtered from ${allPendingUploads.size})")

            for (tab in pendingDeletes) {
                if (tab.userId != userId) continue

                Log.d(TAG, "Tab pending deletion with URL: ${tab.url}, serverId: ${tab.serverId}, userId: ${tab.userId}")

                try {
                    // Extract original URL if needed
                    val originalUrl = if (tab.url.startsWith("PENDING_DELETE:")) {
                        tab.url.removePrefix("PENDING_DELETE:")
                    } else {
                        tab.url
                    }

                    var deletedFromServer = false

                    if (!tab.serverId.isNullOrEmpty()) {
                        // If we have server ID, delete by ID
                        try {
                            tabApiService.deleteTab(
                                authorization = "Bearer $token",
                                id = tab.serverId
                            )
                            deletedFromServer = true
                            Log.d(TAG, "Successfully deleted tab from server by ID: ${tab.serverId}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to delete tab by ID: ${e.message}")
                        }
                    }

                    if (!deletedFromServer) {
                        // If no server ID or deletion failed, try to find by URL
                        try {
                            val response = tabApiService.getAllTabs("Bearer $token")
                            val matchingTab = response.data.find { it.url == originalUrl }

                            if (matchingTab?.id != null) {
                                tabApiService.deleteTab(
                                    authorization = "Bearer $token",
                                    id = matchingTab.id
                                )
                                deletedFromServer = true
                                Log.d(TAG, "Successfully deleted tab by URL lookup: $originalUrl")
                            } else {
                                // If not found, consider it deleted
                                deletedFromServer = true
                                Log.d(TAG, "No matching tab found on server for URL: $originalUrl")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error finding/deleting tab by URL: ${e.message}")
                        }
                    }

                    // Remove local shadow entry if successful
                    if (deletedFromServer) {
                        tabRepository.deleteTab(tab)
                        Log.d(TAG, "Removed local shadow entry for deleted tab: ${tab.url}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing tab deletion: ${e.message}")
                }
            }

            // Handle tabs pending upload
            for (tab in pendingUploads) {
                if (tab.userId != userId) continue

                try {
                    val dto = tab.toDto(userId, deviceId)
                    Log.d(TAG, "Pushing tab to server: ${tab.url}")

                    val response = tabApiService.addTab(
                        authorization = "Bearer $token",
                        tab = dto
                    )

                    val serverId = response.data.id
                    tabRepository.markAsSynced(tab, serverId)
                    Log.d(TAG, "Successfully uploaded tab: ${tab.url}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error uploading tab ${tab.url}: ${e.message}")
                }
            }
            Log.d(TAG, "Completed pushing pending tabs")
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing pending tabs: ${e.message}")
            throw e
        }
    }

    /**
     * Pulls tabs from the remote server.
     */
    private suspend fun pullRemoteTabs(token: String, userId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Pulling remote tabs from server")
                val response = tabApiService.getAllTabs("Bearer $token")
                val remoteTabs = response.data
                Log.d(TAG, "Received ${remoteTabs.size} tab entries from server")

                // Get tabs pending deletion to avoid re-adding
                val pendingDeletions = tabRepository.getPendingDeletes()
                val pendingDeletionUrls = pendingDeletions.map {
                    if (it.url.startsWith("PENDING_DELETE:")) {
                        it.url.removePrefix("PENDING_DELETE:")
                    } else {
                        it.url
                    }
                }

                remoteTabs.forEach { remoteTab ->
                    try {
                        // Skip entries with empty URLs
                        if (remoteTab.url.isBlank()) {
                            Log.d(TAG, "Skipping tab with empty URL")
                            return@forEach
                        }

                        // Skip tabs pending deletion locally
                        if (pendingDeletionUrls.contains(remoteTab.url)) {
                            Log.d(TAG, "Skipping tab ${remoteTab.url} as it's pending deletion locally")
                            return@forEach
                        }

                        val localTab = tabDao.getTabByUrl(remoteTab.url)
                        if (localTab == null) {
                            tabRepository.insertTabFromDto(remoteTab, userId)
                            Log.d(TAG, "Added new tab from server: ${remoteTab.url}")
                        } else if (localTab.syncStatus != SyncStatus.PENDING_UPLOAD) {
                            tabRepository.updateTabFromDto(remoteTab, userId)
                            Log.d(TAG, "Updated local tab from server: ${remoteTab.url}")
                        } else {
                            tabRepository.updateTabFromDto(remoteTab, userId)
                            Log.d(TAG, "Local tab ${remoteTab.url} has pending changes. Skipping update.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing remote tab ${remoteTab.url}: ${e.message}")
                        e.printStackTrace()
                    }
                }
                Log.d(TAG, "Completed pulling remote tabs")
            } catch (e: Exception) {
                Log.e(TAG, "Error pulling remote tabs: ${e.message}")
                throw e
            }
        }
    }
}