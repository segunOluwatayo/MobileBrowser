package com.example.mobilebrowser.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.entity.BookmarkEntity
import com.example.mobilebrowser.data.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Date

@HiltViewModel
class BookmarkViewModel @Inject constructor(
    private val repository: BookmarkRepository
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
            val bookmark = BookmarkEntity(
                title = title,
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
}
