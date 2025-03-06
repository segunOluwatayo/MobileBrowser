package com.example.mobilebrowser.ui.viewmodels

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.entity.BookmarkEntity
import com.example.mobilebrowser.data.repository.BookmarkRepository
import com.example.mobilebrowser.data.util.BookmarkThumbnailService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import java.util.Date
import dagger.hilt.android.qualifiers.ApplicationContext


@HiltViewModel
class BookmarkViewModel @Inject constructor(
    private val repository: BookmarkRepository,
    private val thumbnailService: BookmarkThumbnailService,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // StateFlow to store the current search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // StateFlow to store the filtered or all bookmarks, depending on the search query
    val bookmarks = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllBookmarks()
            } else {
                repository.searchBookmarks(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // StateFlow to store the current URL being viewed
    private val _currentUrl = MutableStateFlow<String?>(null)
    val currentUrl: StateFlow<String?> = _currentUrl.asStateFlow()

    // StateFlow to track if the current URL is bookmarked
    private val _isCurrentUrlBookmarked = MutableStateFlow(false)
    val isCurrentUrlBookmarked: StateFlow<Boolean> = _isCurrentUrlBookmarked.asStateFlow()

    // Updates the current search query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Updates the current URL and checks if it's bookmarked
    fun updateCurrentUrl(url: String) {
        viewModelScope.launch {
            _currentUrl.value = url
            _isCurrentUrlBookmarked.value = repository.isUrlBookmarked(url)
        }
    }

    // Fetches a bookmark by its ID
    suspend fun getBookmarkById(id: Long): BookmarkEntity? {
        return repository.getBookmarkById(id)
    }

    // Adds a new bookmark and updates the bookmark state for the current URL
    fun addBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            repository.addBookmark(bookmark)
            _isCurrentUrlBookmarked.value = true
        }
    }

    // Quickly adds a new bookmark using just a URL and title
    fun quickAddBookmark(url: String, title: String) {
        viewModelScope.launch {
            // Use the URL's domain as fallback title if title is empty
            val bookmarkTitle = if (title.isBlank()) {
                try {
                    java.net.URL(url).host.removePrefix("www.")
                } catch (e: Exception) {
                    "Untitled"
                }
            } else {
                title
            }

            val bookmark = BookmarkEntity(
                title = bookmarkTitle,
                url = url,
                favicon = null, // TODO: Implement favicon fetching
                lastVisited = Date(),
                tags = null // No tags by default
            )
            repository.addBookmark(bookmark)
            _isCurrentUrlBookmarked.value = true
        }
    }

    // Updates an existing bookmark
    fun updateBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            repository.updateBookmark(bookmark)
        }
    }

    // Deletes a bookmark and updates the bookmark state for the current URL
    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            repository.deleteBookmark(bookmark)
            if (bookmark.url == _currentUrl.value) {
                _isCurrentUrlBookmarked.value = false
            }
        }
    }
    /**
     * Generate thumbnails for bookmarks that don't have them yet
     */
    fun generateThumbnails() {
//        viewModelScope.launch {
//            repository.getAllBookmarks().first().forEach { bookmark ->
//                if (bookmark.favicon.isNullOrEmpty() || !bookmark.favicon.startsWith("file://")) {
//                    // Try to generate a thumbnail
//                    thumbnailService.generateThumbnailForBookmark(bookmark)
//                }
//            }
//        }
    }

    /**
     * Get or generate thumbnail for a specific bookmark
     */
    fun getThumbnailForBookmark(bookmarkId: Long) {
//        viewModelScope.launch {
//            repository.getBookmarkById(bookmarkId)?.let { bookmark ->
//                if (bookmark.favicon.isNullOrEmpty() || !bookmark.favicon.startsWith("file://")) {
//                    // Try to generate a thumbnail
//                    thumbnailService.generateThumbnailForBookmark(bookmark)
//                }
//            }
//        }
    }

    /**
     * Capture thumbnail when adding a new bookmark from the current webpage
     */
    fun quickAddBookmarkWithThumbnail(url: String, title: String, thumbnail: Bitmap?) {
        viewModelScope.launch {
            // Create and add the bookmark
            val bookmark = BookmarkEntity(
                title = title.ifBlank() {
                    try {
                        java.net.URL(url).host.removePrefix("www.")
                    } catch (e: Exception) {
                        "Untitled"
                    }
                },
                url = url,
                favicon = null,
                lastVisited = Date(),
                tags = null
            )

            val bookmarkId = repository.addBookmark(bookmark)

            // If we have a thumbnail bitmap, save it
            if (thumbnail != null) {
                val thumbnailFile = File(context.cacheDir, "bookmark_thumbnails/bookmark_${bookmarkId}.png")
                thumbnailFile.parentFile?.mkdirs()

                FileOutputStream(thumbnailFile).use { out ->
                    thumbnail.compress(Bitmap.CompressFormat.PNG, 90, out)
                }

                // Update the bookmark with the thumbnail path
                repository.getBookmarkById(bookmarkId)?.let { savedBookmark ->
                    repository.updateBookmark(
                        savedBookmark.copy(
                            favicon = "file://${thumbnailFile.absolutePath}"
                        )
                    )
                }
            } else {
                // Try to generate a thumbnail
                repository.getBookmarkById(bookmarkId)?.let { savedBookmark ->
//                    thumbnailService.generateThumbnailForBookmark(savedBookmark)
                }
            }

            _isCurrentUrlBookmarked.value = true
        }
    }
}
