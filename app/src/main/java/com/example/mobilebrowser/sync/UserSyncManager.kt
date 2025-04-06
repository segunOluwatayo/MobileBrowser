//package com.example.mobilebrowser.sync
//
//import android.util.Log
//import com.example.mobilebrowser.api.HistoryApiService
//import com.example.mobilebrowser.data.dto.ApiResponse
//import com.example.mobilebrowser.data.dto.HistoryDto
//import com.example.mobilebrowser.data.entity.SyncStatus
//import com.example.mobilebrowser.data.repository.HistoryRepository
//import com.example.mobilebrowser.data.repository.toDto
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.withContext
//import javax.inject.Inject
//import javax.inject.Singleton
//
//sealed class SyncStatusState {
//    object Idle : SyncStatusState()
//    object Syncing : SyncStatusState()
//    object Synced : SyncStatusState()
//    data class Error(val message: String) : SyncStatusState()
//}
//
//@Singleton
//class UserSyncManager @Inject constructor(
//    private val historyRepository: HistoryRepository,
//    private val historyApiService: HistoryApiService
//) {
//    private val TAG = "UserSyncManager"
//
//    /**
//     * Deletes a history entry from the server by its server ID.
//     * Throws an Exception if it fails (so you can catch/handle it).
//     */
//    suspend fun deleteHistoryEntryFromServer(historyId: String, accessToken: String) {
//        try {
//            historyApiService.deleteHistoryEntry("Bearer $accessToken", historyId)
//        } catch (e: Exception) {
//            throw Exception("Failed to delete history from server: ${e.message}")
//        }
//    }
//
//    /**
//     * Pushes all local history changes (pending deletions and pending uploads) to the server.
//     *
//     * 1) Pending Deletions:
//     *    - If serverId exists, delete by serverId.
//     *    - Otherwise try to delete by URL (removing "PENDING_DELETE:" prefix if present).
//     *    - If successful (or 404), remove locally so it won't return on next sync.
//     *
//     * 2) Pending Uploads:
//     *    - POST each item to the server, then mark as SYNCED with the new serverId.
//     */
//    suspend fun pushLocalChanges(accessToken: String, deviceId: String, userId: String) {
//        withContext(Dispatchers.IO) {
//            /** -- 1) Handle PENDING_DELETE items -- **/
//            try {
//                val pendingDeletes = historyRepository.getAllHistoryAsList()
//                    .filter { it.syncStatus == SyncStatus.PENDING_DELETE }
//
//                Log.d(TAG, "Found ${pendingDeletes.size} history entries pending deletion")
//
//                for (deleteItem in pendingDeletes) {
//                    if (deleteItem.userId != userId) continue
//
//                    try {
//                        if (!deleteItem.serverId.isNullOrBlank()) {
//                            historyApiService.deleteHistoryEntry("Bearer $accessToken", deleteItem.serverId)
//                            Log.d(TAG, "Deleted item from server by ID: ${deleteItem.url}")
//                        } else {
//                            val originalUrl = if (deleteItem.url.startsWith("PENDING_DELETE:")) {
//                                deleteItem.url.removePrefix("PENDING_DELETE:")
//                            } else {
//                                deleteItem.url
//                            }
//
//                            try {
//                                historyApiService.deleteHistoryEntryByUrl("Bearer $accessToken", originalUrl)
//                                Log.d(TAG, "Deleted item from server by URL: $originalUrl")
//                            } catch (notFound: Exception) {
//                                Log.d(TAG, "Could not find item on server for url: $originalUrl. Ignoring.")
//                            }
//                        }
//
//                        historyRepository.finalizeDeletion(deleteItem)
//
//                    } catch (deleteError: Exception) {
//                        Log.e(TAG, "Error deleting item ${deleteItem.url} from server: ${deleteError.message}", deleteError)
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Error handling pending deletions: ${e.message}", e)
//            }
//
//            /** -- 2) Handle PENDING_UPLOAD items -- **/
//            try {
//                val pendingUploads = historyRepository.getPendingUploads().first()
//                Log.d(TAG, "Found ${pendingUploads.size} history entries pending upload")
//
//                for (entry in pendingUploads) {
//                    if (entry.userId != userId) continue
//
//                    try {
//                        val dto = entry.toDto(deviceId)
//                        val response = historyApiService.addHistoryEntry("Bearer $accessToken", dto)
//                        val serverId = response.data.id
//
//                        historyRepository.markAsSynced(entry, serverId)
//                        Log.d(TAG, "Successfully uploaded: ${entry.url}")
//                    } catch (uploadError: Exception) {
//                        Log.e(TAG, "Error uploading entry ${entry.url}: ${uploadError.message}", uploadError)
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Error handling pending uploads: ${e.message}", e)
//                throw e
//            }
//        }
//    }
//}
package com.example.mobilebrowser.sync

import android.util.Log
import com.example.mobilebrowser.api.BookmarkApiService
import com.example.mobilebrowser.api.HistoryApiService
import com.example.mobilebrowser.api.TabApiService
import com.example.mobilebrowser.data.dao.TabDao
import com.example.mobilebrowser.data.dto.ApiResponse
import com.example.mobilebrowser.data.dto.BookmarkDto
import com.example.mobilebrowser.data.dto.HistoryDto
import com.example.mobilebrowser.data.entity.SyncStatus
import com.example.mobilebrowser.data.repository.BookmarkRepository
import com.example.mobilebrowser.data.repository.HistoryRepository
import com.example.mobilebrowser.data.repository.TabRepository
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
    // History dependencies
    private val historyRepository: HistoryRepository,
    private val historyApiService: HistoryApiService,
    // Bookmark dependencies
    private val bookmarkRepository: BookmarkRepository,
    private val bookmarkApiService: BookmarkApiService,
    private val tabRepository: TabRepository,
    private val tabDao: TabDao,
    private val tabApiService: TabApiService
) {
    private val TAG = "UserSyncManager"

    // ---------- History Sync Functions (Existing) ----------

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
     */
    suspend fun pushLocalChanges(accessToken: String, deviceId: String, userId: String) {
        withContext(Dispatchers.IO) {
            /** -- 1) Handle PENDING_DELETE items -- **/
            try {
                val pendingDeletes = historyRepository.getAllHistoryAsList()
                    .filter { it.syncStatus == SyncStatus.PENDING_DELETE }

                Log.d(TAG, "Found ${pendingDeletes.size} history entries pending deletion")

                for (deleteItem in pendingDeletes) {
                    if (deleteItem.userId != userId) continue

                    try {
                        if (!deleteItem.serverId.isNullOrBlank()) {
                            historyApiService.deleteHistoryEntry("Bearer $accessToken", deleteItem.serverId)
                            Log.d(TAG, "Deleted item from server by ID: ${deleteItem.url}")
                        } else {
                            val originalUrl = if (deleteItem.url.startsWith("PENDING_DELETE:")) {
                                deleteItem.url.removePrefix("PENDING_DELETE:")
                            } else {
                                deleteItem.url
                            }

                            try {
                                historyApiService.deleteHistoryEntryByUrl("Bearer $accessToken", originalUrl)
                                Log.d(TAG, "Deleted item from server by URL: $originalUrl")
                            } catch (notFound: Exception) {
                                Log.d(TAG, "Could not find item on server for url: $originalUrl. Ignoring.")
                            }
                        }

                        historyRepository.finalizeDeletion(deleteItem)

                    } catch (deleteError: Exception) {
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
                    if (entry.userId != userId) continue

                    try {
                        val dto = entry.toDto(deviceId)
                        val response = historyApiService.addHistoryEntry("Bearer $accessToken", dto)
                        val serverId = response.data.id

                        historyRepository.markAsSynced(entry, serverId)
                        Log.d(TAG, "Successfully uploaded: ${entry.url}")
                    } catch (uploadError: Exception) {
                        Log.e(TAG, "Error uploading entry ${entry.url}: ${uploadError.message}", uploadError)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling pending uploads: ${e.message}", e)
                throw e
            }
        }
    }

    // ---------- Bookmark Sync Functions (New) ----------

    /**
     * Pushes all local bookmark changes (pending deletions and pending uploads) to the server.
     */
    suspend fun pushLocalBookmarkChanges(accessToken: String, deviceId: String, userId: String) {
        withContext(Dispatchers.IO) {
            // --- Handle Bookmark Pending Deletions ---
            try {
                val pendingDeletions = bookmarkRepository.getPendingDeletions()
                Log.d(TAG, "Found ${pendingDeletions.size} bookmarks pending deletion")

                for (bookmark in pendingDeletions) {
                    if (bookmark.userId != userId) continue

                    try {
                        // Extract original URL if it has the PENDING_DELETE prefix
                        val originalUrl = if (bookmark.url.startsWith("PENDING_DELETE:")) {
                            bookmark.url.removePrefix("PENDING_DELETE:")
                        } else {
                            bookmark.url
                        }

                        var deletedFromServer = false

                        if (!bookmark.serverId.isNullOrEmpty()) {
                            // If we have server ID, delete by ID
                            Log.d(TAG, "Deleting bookmark from server by ID: ${bookmark.serverId}")
                            try {
                                bookmarkApiService.deleteBookmark(
                                    authorization = "Bearer $accessToken",
                                    id = bookmark.serverId
                                )
                                deletedFromServer = true
                                Log.d(TAG, "Successfully deleted bookmark from server by ID: ${bookmark.url}")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to delete bookmark by ID: ${e.message}")
                            }
                        }

                        if (!deletedFromServer) {
                            // If no server ID or deletion by ID failed, find and delete by URL
                            Log.d(TAG, "Attempting to find and delete bookmark by URL: $originalUrl")

                            try {
                                // 1. Get all bookmarks from server
                                val response = bookmarkApiService.getAllBookmarks("Bearer $accessToken")

                                // 2. Find the bookmark with matching URL
                                val matchingBookmark = response.data.find { it.url == originalUrl }

                                if (matchingBookmark?.id != null) {
                                    // 3. Delete the bookmark using its server ID
                                    Log.d(TAG, "Found matching bookmark on server with ID: ${matchingBookmark.id}")
                                    bookmarkApiService.deleteBookmark(
                                        authorization = "Bearer $accessToken",
                                        id = matchingBookmark.id
                                    )
                                    deletedFromServer = true
                                    Log.d(TAG, "Successfully deleted bookmark by URL lookup: $originalUrl")
                                } else {
                                    Log.d(TAG, "No matching bookmark found on server for URL: $originalUrl")
                                    // If bookmark doesn't exist on server, consider it "deleted"
                                    deletedFromServer = true
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error finding/deleting bookmark by URL: ${e.message}")
                            }
                        }

                        // Only remove local shadow entry if we successfully deleted from server
                        // or confirmed it doesn't exist on server
                        if (deletedFromServer) {
                            // Remove bookmark locally upon successful deletion
                            bookmarkRepository.deleteBookmark(bookmark)
                            Log.d(TAG, "Removed local shadow entry for deleted bookmark: ${bookmark.url}")
                        } else {
                            Log.d(TAG, "Keeping shadow entry for retry: ${bookmark.url}")
                        }
                    } catch (deleteError: Exception) {
                        Log.e(TAG, "Error processing deletion for ${bookmark.url}: ${deleteError.message}", deleteError)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling bookmark pending deletions: ${e.message}", e)
            }

            // --- Handle Bookmark Pending Uploads ---
            try {
                val pendingUploads = bookmarkRepository.getPendingUploads()
                Log.d(TAG, "Found ${pendingUploads.size} bookmarks pending upload")
                for (bookmark in pendingUploads) {
                    if (bookmark.userId != userId) continue
                    try {
                        val dto = bookmark.toDto(userId)
                        Log.d(TAG, "Pushing bookmark to server: ${bookmark.url}")
                        val response: ApiResponse<BookmarkDto> = bookmarkApiService.addBookmark(
                            authorization = "Bearer $accessToken",
                            bookmark = dto
                        )
                        val serverId = response.data.id
                        bookmarkRepository.markAsSynced(bookmark, serverId)
                        Log.d(TAG, "Successfully uploaded bookmark: ${bookmark.url}")
                    } catch (uploadError: Exception) {
                        Log.e(TAG, "Error uploading bookmark ${bookmark.url}: ${uploadError.message}", uploadError)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling bookmark pending uploads: ${e.message}", e)
                throw e
            }
        }
    }

    /**
     * Pushes all local tab changes to the server.
     */
    suspend fun pushLocalTabChanges(accessToken: String, deviceId: String, userId: String) {
        withContext(Dispatchers.IO) {
            // Handle tabs pending deletion
            try {
                val pendingDeletes = tabRepository.getPendingDeletes()
                Log.d(TAG, "Found ${pendingDeletes.size} tabs pending deletion")

                for (tab in pendingDeletes) {
                    if (tab.userId != userId) continue

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
                                    authorization = "Bearer $accessToken",
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
                                val response = tabApiService.getAllTabs("Bearer $accessToken")
                                val matchingTab = response.data.find { it.url == originalUrl }

                                if (matchingTab?.id != null) {
                                    tabApiService.deleteTab(
                                        authorization = "Bearer $accessToken",
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
            } catch (e: Exception) {
                Log.e(TAG, "Error handling tab pending deletions: ${e.message}")
            }

            // Handle tabs pending upload
            try {
                val pendingUploads = tabRepository.getPendingUploads()
                Log.d(TAG, "Found ${pendingUploads.size} tabs pending upload")

                for (tab in pendingUploads) {
                    if (tab.userId != userId) continue

                    try {
                        val dto = tab.toDto(userId, deviceId)
                        Log.d(TAG, "Pushing tab to server: ${tab.url}")

                        val response = tabApiService.addTab(
                            authorization = "Bearer $accessToken",
                            tab = dto
                        )

                        val serverId = response.data.id
                        tabRepository.markAsSynced(tab, serverId)
                        Log.d(TAG, "Successfully uploaded tab: ${tab.url}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error uploading tab ${tab.url}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling tab pending uploads: ${e.message}")
            }
        }
    }

    /**
     * Pulls remote tabs from the server and updates the local database.
     */
    suspend fun pullRemoteTabs(accessToken: String, userId: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Pulling remote tabs from server")
                val response = tabApiService.getAllTabs("Bearer $accessToken")
                val remoteTabs = response.data
                Log.d(TAG, "Received ${remoteTabs.size} tabs from server")

                // Get local tabs pending deletion to avoid re-adding
                val pendingDeletions = tabRepository.getPendingDeletes()
                val pendingDeletionUrls = pendingDeletions.map {
                    if (it.url.startsWith("PENDING_DELETE:")) {
                        it.url.removePrefix("PENDING_DELETE:")
                    } else {
                        it.url
                    }
                }

                for (remoteTab in remoteTabs) {
                    try {
                        // Skip tabs pending deletion locally
                        if (pendingDeletionUrls.contains(remoteTab.url)) {
                            Log.d(TAG, "Skipping tab ${remoteTab.url} as it's pending deletion locally")
                            continue
                        }

                        val localTab = tabDao.getTabByUrl(remoteTab.url)
                        if (localTab == null) {
                            // Insert new tab from server
                            tabRepository.insertTabFromDto(remoteTab, userId)
                            Log.d(TAG, "Inserted new tab from server: ${remoteTab.url}")
                        } else if (localTab.syncStatus != SyncStatus.PENDING_UPLOAD) {
                            // Update existing tab if remote is newer
                            tabRepository.updateTabFromDto(remoteTab, userId)
                            Log.d(TAG, "Updated local tab from server: ${remoteTab.url}")
                        } else {
                            Log.d(TAG, "Local tab ${remoteTab.url} has pending changes. Skipping update.")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing remote tab ${remoteTab.url}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pulling remote tabs: ${e.message}")
                throw e
            }
        }
    }
}

    /**
     * Pulls remote bookmarks from the server and updates the local database.
     * Inserts new bookmarks or updates existing ones if the remote data is newer.
     */
//    suspend fun pullRemoteBookmarks(accessToken: String) {
//        withContext(Dispatchers.IO) {
//            try {
//                Log.d(TAG, "Pulling remote bookmarks from server")
//                val response: ApiResponse<List<BookmarkDto>> = bookmarkApiService.getAllBookmarks(
//                    authorization = "Bearer $accessToken"
//                )
//                val remoteBookmarks = response.data
//                Log.d(TAG, "Received ${remoteBookmarks.size} bookmarks from server")
//                for (remoteBookmark in remoteBookmarks) {
//                    try {
//                        val localBookmark = bookmarkRepository.getBookmarkByUrl(remoteBookmark.url)
//                        if (localBookmark == null) {
//                            // Insert new bookmark
//                            val newBookmark = com.example.mobilebrowser.data.entity.BookmarkEntity(
//                                title = remoteBookmark.title,
//                                url = remoteBookmark.url,
//                                favicon = remoteBookmark.favicon,
//                                lastVisited = remoteBookmark.timestamp,
//                                tags = remoteBookmark.tags,
//                                dateAdded = remoteBookmark.timestamp,
//                                serverId = remoteBookmark.id,
//                                syncStatus = SyncStatus.SYNCED
//                            )
//                            bookmarkRepository.addBookmark(newBookmark)
//                            Log.d(TAG, "Inserted new bookmark from server: ${remoteBookmark.url}")
//                        } else if (localBookmark.syncStatus != SyncStatus.PENDING_UPLOAD) {
//                            if (remoteBookmark.timestamp.after(localBookmark.dateAdded)) {
//                                val updatedBookmark = localBookmark.copy(
//                                    title = remoteBookmark.title,
//                                    url = remoteBookmark.url,
//                                    favicon = remoteBookmark.favicon,
//                                    lastVisited = remoteBookmark.timestamp,
//                                    tags = remoteBookmark.tags,
//                                    dateAdded = remoteBookmark.timestamp,
//                                    serverId = remoteBookmark.id,
//                                    syncStatus = SyncStatus.SYNCED
//                                )
//                                bookmarkRepository.updateBookmark(updatedBookmark)
//                                Log.d(TAG, "Updated local bookmark from server: ${remoteBookmark.url}")
//                            }
//                        } else {
//                            Log.d(TAG, "Local bookmark ${remoteBookmark.url} has pending changes. Skipping update.")
//                        }
//                    } catch (processError: Exception) {
//                        Log.e(TAG, "Error processing remote bookmark ${remoteBookmark.url}: ${processError.message}", processError)
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Error pulling remote bookmarks: ${e.message}", e)
//                throw e
//            }
//        }
//    }


