package com.example.mobilebrowser.data.dao

import androidx.room.*
import com.example.mobilebrowser.data.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    // Retrieve all bookmarks from the database, ordered by date added (most recent first).
    @Query("SELECT * FROM bookmarks ORDER BY dateAdded DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    // Retrieve all bookmarks as a List (not a Flow) for sync operations.
    @Query("SELECT * FROM bookmarks ORDER BY dateAdded DESC")
    suspend fun getAllBookmarksAsList(): List<BookmarkEntity>

    // Fetch a single bookmark by its unique ID.
    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getBookmarkById(id: Long): BookmarkEntity?

    // Insert a new bookmark into the database.
    @Insert
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    // Update an existing bookmark in the database.
    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    // Delete a bookmark from the database.
    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    // Check if a specific URL is already bookmarked.
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    suspend fun isUrlBookmarked(url: String): Boolean

    // Search for bookmarks whose title or URL matches the given query string.
    @Query("SELECT * FROM bookmarks WHERE title LIKE :query OR url LIKE :query")
    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>>

    // Retrieve bookmarks filtered by a specific tag.
    @Query("SELECT * FROM bookmarks WHERE tags LIKE :tag")
    fun getBookmarksByTag(tag: String): Flow<List<BookmarkEntity>>

    // Retrieve a single bookmark by its URL.
    @Query("SELECT * FROM bookmarks WHERE url = :url LIMIT 1")
    suspend fun getBookmarkByUrl(url: String): BookmarkEntity?
}
