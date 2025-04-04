package com.example.mobilebrowser.data.repository

import android.util.Log
import com.example.mobilebrowser.api.BookmarkApiService
import com.example.mobilebrowser.data.dao.BookmarkDao
import com.example.mobilebrowser.data.dto.BookmarkDto
import com.example.mobilebrowser.data.entity.BookmarkEntity
import com.example.mobilebrowser.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao,
    private val bookmarkApiService: BookmarkApiService
) {

    // Fetch all bookmarks, ordered by date added (most recent first)
    fun getAllBookmarks(): Flow<List<BookmarkEntity>> =
        bookmarkDao.getAllBookmarks()
            .map { bookmarks ->
                bookmarks.filter { !it.url.startsWith("PENDING_DELETE:") }
            }

    // Retrieve a bookmark by its ID
    suspend fun getBookmarkById(id: Long): BookmarkEntity? = bookmarkDao.getBookmarkById(id)

    // Add a new bookmark to the database
    suspend fun addBookmark(bookmark: BookmarkEntity): Long {
        Log.d("BookmarkRepository", "Adding bookmark: '${bookmark.title}', URL: ${bookmark.url}, UserID: ${bookmark.userId}")
        val id = bookmarkDao.insertBookmark(bookmark)
        Log.d("BookmarkRepository", "Added bookmark with ID: $id")
        return id
    }

    // Update an existing bookmark in the database
    suspend fun updateBookmark(bookmark: BookmarkEntity) = bookmarkDao.updateBookmark(bookmark)

    // Delete a bookmark from the database (for anonymous users or after sync deletion)
    suspend fun deleteBookmark(bookmark: BookmarkEntity) = bookmarkDao.deleteBookmark(bookmark)

    // Check if a specific URL is already bookmarked
    suspend fun isUrlBookmarked(url: String): Boolean = bookmarkDao.isUrlBookmarked(url)

    // Search for bookmarks whose title or URL matches the given query string
    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>> =
        bookmarkDao.searchBookmarks("%$query%")
            .map { bookmarks ->
                bookmarks.filter { !it.url.startsWith("PENDING_DELETE:") }
            }

    // Fetch bookmarks associated with a specific tag
    fun getBookmarksByTag(tag: String): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarksByTag("%$tag%")

    // Retrieve a bookmark by its URL
    suspend fun getBookmarkByUrl(url: String): BookmarkEntity? {
        val result = bookmarkDao.getBookmarkByUrl(url)
        Log.d("BookmarkRepository", "getBookmarkByUrl($url) returned: ${result?.id ?: "null"}")
        return result
    }

    // Retrieve all bookmarks as a List (non-reactive), used for synchronization
    suspend fun getAllBookmarksAsList(): List<BookmarkEntity> {
        val bookmarks = bookmarkDao.getAllBookmarksAsList()
        Log.d("BookmarkRepository", "getAllBookmarksAsList() returned ${bookmarks.size} bookmarks")
        return bookmarks
    }

    // Retrieve bookmarks pending upload
    suspend fun getPendingUploads(): List<BookmarkEntity> {
        return getAllBookmarksAsList().filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
    }

    // Retrieve bookmarks pending deletion
    suspend fun getPendingDeletions(): List<BookmarkEntity> {
        return getAllBookmarksAsList().filter {
            it.syncStatus == SyncStatus.PENDING_DELETE || it.url.startsWith("PENDING_DELETE:")
        }
    }

    /**
     * Mark a bookmark as synced after a successful API call.
     *
     * @param bookmark The local bookmark entry.
     * @param serverId The server-assigned ID returned by the backend.
     */
    suspend fun markAsSynced(bookmark: BookmarkEntity, serverId: String?) {
        val updatedBookmark = bookmark.copy(
            syncStatus = SyncStatus.SYNCED,
            serverId = serverId
        )
        bookmarkDao.updateBookmark(updatedBookmark)
    }

    /**
     * Mark a bookmark for deletion (used when the user is signed in).
     * The bookmark remains in the local database with a status of PENDING_DELETE.
     *
     * @param bookmark The bookmark to be marked for deletion.
     */
    suspend fun markForDeletion(bookmark: BookmarkEntity) {
        val updatedBookmark = bookmark.copy(syncStatus = SyncStatus.PENDING_DELETE)
        bookmarkDao.updateBookmark(updatedBookmark)
    }
    
    suspend fun deleteBookmarkImmediate(
        bookmark: BookmarkEntity,
        isUserSignedIn: Boolean,
        accessToken: String,
        deviceId: String
    ) {
        if (isUserSignedIn && bookmark.userId.isNotBlank()) {
            try {
                // If we have a server ID, try to delete directly on server
                if (!bookmark.serverId.isNullOrBlank()) {
                    bookmarkApiService.deleteBookmark("Bearer $accessToken", bookmark.serverId)
                    // Success! Remove locally
                    bookmarkDao.deleteBookmark(bookmark)
                } else {
                    // No server ID, we need to track for deletion but hide from UI

                    // Create a shadow entry for deletion tracking
                    // Use a special prefix in URL so it won't show in UI queries
                    // Set id=0 to let Room auto-generate a new ID
                    val shadowEntry = bookmark.copy(
                        id = 0, // Important: Reset ID to avoid primary key conflict
                        url = "PENDING_DELETE:" + bookmark.url,
                        syncStatus = SyncStatus.PENDING_DELETE
                    )

                    // Insert the shadow tracking entry
                    bookmarkDao.insertBookmark(shadowEntry)

                    // Delete the original entry so it disappears from UI
                    bookmarkDao.deleteBookmark(bookmark)
                }
            } catch (e: Exception) {
                Log.e("BookmarkRepository", "Error deleting bookmark from server: ${e.message}", e)

                // API failure - create shadow entry and delete original
                val shadowEntry = bookmark.copy(
                    id = 0, // Important: Reset ID to avoid primary key conflict
                    url = "PENDING_DELETE:" + bookmark.url,
                    syncStatus = SyncStatus.PENDING_DELETE
                )
                bookmarkDao.insertBookmark(shadowEntry)
                bookmarkDao.deleteBookmark(bookmark)
            }
        } else {
            // For anonymous users, just delete locally
            bookmarkDao.deleteBookmark(bookmark)
        }
    }
}

/**
 * Extension function to convert a BookmarkEntity to a BookmarkDto.
 * @param userId The identifier of the user owning this bookmark.
 */
fun BookmarkEntity.toDto(userId: String): BookmarkDto {
    return BookmarkDto(
        id = this.serverId,
        userId = userId,
        title = this.title,
        url = this.url,
        favicon = this.favicon,
        tags = this.tags,
        timestamp = this.dateAdded
    )
}
