package com.example.mobilebrowser.data.repository

import com.example.mobilebrowser.data.dao.BookmarkDao
import com.example.mobilebrowser.data.entity.BookmarkEntity
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
    suspend fun addBookmark(bookmark: BookmarkEntity) = bookmarkDao.insertBookmark(bookmark)

    // Update an existing bookmark in the database
    suspend fun updateBookmark(bookmark: BookmarkEntity) = bookmarkDao.updateBookmark(bookmark)

    // Delete a bookmark from the database
    suspend fun deleteBookmark(bookmark: BookmarkEntity) = bookmarkDao.deleteBookmark(bookmark)

    // Check if a specific URL is already bookmarked
    suspend fun isUrlBookmarked(url: String): Boolean = bookmarkDao.isUrlBookmarked(url)

    // Search for bookmarks whose title or URL matches the given query string
    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>> =
        bookmarkDao.searchBookmarks("%$query%")

    // Fetch bookmarks associated with a specific tag
    fun getBookmarksByTag(tag: String): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarksByTag("%$tag%")
}
