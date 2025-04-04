package com.example.mobilebrowser.data.repository

import com.example.mobilebrowser.data.dao.BookmarkDao
import com.example.mobilebrowser.data.dto.BookmarkDto
import com.example.mobilebrowser.data.entity.BookmarkEntity
import com.example.mobilebrowser.data.entity.SyncStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao
) {

    // Fetch all bookmarks, ordered by date added (most recent first)
    fun getAllBookmarks(): Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    // Retrieve a bookmark by its ID
    suspend fun getBookmarkById(id: Long): BookmarkEntity? = bookmarkDao.getBookmarkById(id)

    // Add a new bookmark to the database
    suspend fun addBookmark(bookmark: BookmarkEntity): Long = bookmarkDao.insertBookmark(bookmark)

    // Update an existing bookmark in the database
    suspend fun updateBookmark(bookmark: BookmarkEntity) = bookmarkDao.updateBookmark(bookmark)

    // Delete a bookmark from the database (for anonymous users or after sync deletion)
    suspend fun deleteBookmark(bookmark: BookmarkEntity) = bookmarkDao.deleteBookmark(bookmark)

    // Check if a specific URL is already bookmarked
    suspend fun isUrlBookmarked(url: String): Boolean = bookmarkDao.isUrlBookmarked(url)

    // Search for bookmarks whose title or URL matches the given query string
    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>> =
        bookmarkDao.searchBookmarks("%$query%")

    // Fetch bookmarks associated with a specific tag
    fun getBookmarksByTag(tag: String): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarksByTag("%$tag%")

    // Retrieve a bookmark by its URL
    suspend fun getBookmarkByUrl(url: String): BookmarkEntity? = bookmarkDao.getBookmarkByUrl(url)

    // Retrieve all bookmarks as a List (non-reactive), used for synchronization
    suspend fun getAllBookmarksAsList(): List<BookmarkEntity> = bookmarkDao.getAllBookmarksAsList()

    // Retrieve bookmarks pending upload
    suspend fun getPendingUploads(): List<BookmarkEntity> {
        return getAllBookmarksAsList().filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
    }

    // Retrieve bookmarks pending deletion
    suspend fun getPendingDeletions(): List<BookmarkEntity> {
        return getAllBookmarksAsList().filter { it.syncStatus == SyncStatus.PENDING_DELETE }
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
