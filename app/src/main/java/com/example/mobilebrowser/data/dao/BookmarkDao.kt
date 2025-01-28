package com.example.mobilebrowser.data.dao
import androidx.room.*
import com.example.mobilebrowser.data.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    // Retrieve all bookmarks from the database, ordered by the date they were added (most recent first).
    @Query("SELECT * FROM bookmarks ORDER BY dateAdded DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    // Fetch a single bookmark by its unique ID.
    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getBookmarkById(id: Long): BookmarkEntity?

    // Insert a new bookmark into the database.
    @Insert
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    // Update an existing bookmark in the database.
    // Requires the BookmarkEntity object to already exist in the database.
    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    // Delete a bookmark from the database.
    // Requires the exact BookmarkEntity object to be passed.
    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    // Check if a specific URL is already bookmarked.
    // Returns true if the URL exists in the database, false otherwise.
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    suspend fun isUrlBookmarked(url: String): Boolean

    // Search for bookmarks whose title or URL matches the given query string.
    // Uses SQL's LIKE operator to perform partial matching.
    @Query("SELECT * FROM bookmarks WHERE title LIKE :query OR url LIKE :query")
    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>>

    // Retrieve bookmarks filtered by a specific tag.
    // Assumes tags are stored as a single string and performs a simple LIKE query for matching.
    @Query("SELECT * FROM bookmarks WHERE tags LIKE :tag")
    fun getBookmarksByTag(tag: String): Flow<List<BookmarkEntity>>
}
