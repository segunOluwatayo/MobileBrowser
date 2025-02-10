package com.example.mobilebrowser.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.example.mobilebrowser.R
import com.example.mobilebrowser.data.model.SearchEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * ViewModel for managing search-related state and logic.
 */
class SearchViewModel : ViewModel() {

    //region State

    /**
     * The currently selected search engine.  Initializes to a default (e.g., Google).
     */
    private val _currentSearchEngine = MutableStateFlow(
        SearchEngine(
            name = "Google",
            logo = R.drawable.google_logo,
            baseUrl = "https://www.google.com/search?q="
        )
    )
    val currentSearchEngine: StateFlow<SearchEngine> = _currentSearchEngine.asStateFlow()

    /**
     * The list of available search engines.
     */
    private val _searchEngines = MutableStateFlow(
        listOf(
            SearchEngine(
                name = "Google",
                logo = R.drawable.google_logo,
                baseUrl = "https://www.google.com/search?q="
            ),
            SearchEngine(
                name = "Bing",
                logo = R.drawable.bing_logo,
                baseUrl = "https://www.bing.com/search?q="
            ),
            SearchEngine(
                name = "DuckDuckGo",
                logo = R.drawable.duckduckgo_logo,
                baseUrl = "https://duckduckgo.com/?q="
            ),
            SearchEngine(
                name = "Qwant",
                logo = R.drawable.qwant_logo,
                baseUrl = "https://www.qwant.com/?q="
            ),
            SearchEngine(
                name = "Wikipedia",
                logo = R.drawable.wikipedia_logo,
                baseUrl = "https://en.wikipedia.org/w/index.php?search="
            ),
            SearchEngine(
                name = "eBay",
                logo = R.drawable.ebay_logo,
                baseUrl = "https://www.ebay.com/sch/i.html?_nkw="
            )
        )
    )
    val searchEngines: StateFlow<List<SearchEngine>> = _searchEngines.asStateFlow()

    private val _isDropdownVisible = MutableStateFlow(false)
    val isDropdownVisible: StateFlow<Boolean> = _isDropdownVisible.asStateFlow()

    /**
     * The current search query entered by the user.
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    //endregion

    //region Actions
    /**
     * Sets the currently selected search engine.
     *
     * @param engine The [SearchEngine] to select.
     */
    fun selectSearchEngine(engine: SearchEngine) {
        _currentSearchEngine.value = engine
    }
    /**
    Set the search query, usually comes from the textfield
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Toggles the visibility of the search engine dropdown.
     */
    fun toggleDropdownVisibility() {
        _isDropdownVisible.update { !it }
    }
    /**
     * Clears the current search query.  Useful when navigating to a new page.
     */
    fun clearSearchQuery() {
        _searchQuery.value = ""
    }
    /**
     *  Set the visibiltity
     */
    fun setDropdownVisibility(isVisible: Boolean) {
        _isDropdownVisible.value = isVisible
    }
}