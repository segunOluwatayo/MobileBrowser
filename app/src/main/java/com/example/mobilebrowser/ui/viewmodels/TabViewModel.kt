package com.example.mobilebrowser.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.entity.TabEntity
import com.example.mobilebrowser.data.repository.TabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TabViewModel @Inject constructor(
    private val repository: TabRepository
) : ViewModel() {
    // Initialization state
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Stream of all tabs
    val tabs = repository.getAllTabs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Stream of the active tab
    val activeTab = repository.getActiveTab()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Stream of tab count
    val tabCount = repository.getTabCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Selection mode state
    private val _isSelectionModeActive = MutableStateFlow(false)
    val isSelectionModeActive = _isSelectionModeActive.asStateFlow()

    private val _selectedTabs = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTabs = _selectedTabs.asStateFlow()

    init {
        // Initialize browser with default tab on startup
        viewModelScope.launch {
            initializeDefaultTab()
        }
    }

    private fun initializeDefaultTab() {
        viewModelScope.launch {
            try {
                val count = repository.getTabCount().first()
                if (count == 0) {
                    Log.d("TabViewModel", "Initializing with default tab...")
                    val newTabId = createTab()
                    switchToTab(newTabId)  // Ensuring it's set as active
                    _isInitialized.value = true
                }
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to initialize browser: ${e.message}")
                _error.value = "Failed to initialize browser: ${e.message}"
            }
        }
    }



    suspend fun createTab(
        url: String = "https://www.mozilla.org",
        title: String = "New Tab"
    ): Long {
        return try {
            val newTabId = repository.createTab(url, title, tabCount.value)
            Log.d("TabViewModel", "Created new tab: $newTabId")
            newTabId
        } catch (e: Exception) {
            Log.e("TabViewModel", "Failed to create tab: ${e.message}")
            throw e
        }
    }


    fun switchToTab(tabId: Long) {
        viewModelScope.launch {
            try {
                repository.switchToTab(tabId)
                Log.d("TabViewModel", "Switched to tab: $tabId")
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to switch tab: ${e.message}")
                _error.value = "Failed to switch tab: ${e.message}"
            }
        }
    }

    fun updateActiveTabContent(url: String, title: String) {
        viewModelScope.launch {
            try {
                activeTab.value?.let { tab ->
                    repository.updateTab(
                        tab.copy(
                            url = url,
                            title = title,
                            lastVisited = Date()
                        )
                    )
                    Log.d("TabViewModel", "Updated tab content - URL: $url, Title: $title")
                }
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to update tab content: ${e.message}")
            }
        }
    }

    fun closeTab(tab: TabEntity) {
        viewModelScope.launch {
            try {
                repository.deleteTab(tab)
                Log.d("TabViewModel", "Closed tab: ${tab.id}")

                // If we closed the last tab, create a new one
                if (tabCount.value == 0) {
                    createTab()
                }
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to close tab: ${e.message}")
            }
        }
    }

    fun closeAllTabs() {
        viewModelScope.launch {
            try {
                repository.deleteAllTabs()
                Log.d("TabViewModel", "Closed all tabs")

                // Ensure a new tab is created and set as active
                val newTabId = createTab()
                switchToTab(newTabId)  // Ensuring it's set as active

            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to close all tabs: ${e.message}")
            }
        }
    }


    private fun toggleSelectionMode() {
        _isSelectionModeActive.value = !_isSelectionModeActive.value
        if (!_isSelectionModeActive.value) {
            clearSelection()
        }
    }

    fun toggleTabSelection(tabId: Long) {
        val currentSelection = _selectedTabs.value.toMutableSet()
        if (currentSelection.contains(tabId)) {
            currentSelection.remove(tabId)
        } else {
            currentSelection.add(tabId)
        }
        _selectedTabs.value = currentSelection
    }

    private fun clearSelection() {
        _selectedTabs.value = emptySet()
    }

    fun closeSelectedTabs() {
        viewModelScope.launch {
            try {
                _selectedTabs.value.forEach { tabId ->
                    repository.getTabById(tabId)?.let { tab ->
                        repository.deleteTab(tab)
                    }
                }
                clearSelection()
                toggleSelectionMode()

                // If we closed all tabs, create a new one
                if (tabCount.value == 0) {
                    createTab()
                }

                Log.d("TabViewModel", "Closed selected tabs")
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to close selected tabs: ${e.message}")
            }
        }
    }

    fun moveTab(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            try {
                val tabList = tabs.value.toMutableList()
                if (fromIndex in tabList.indices && toIndex in tabList.indices) {
                    val tab = tabList.removeAt(fromIndex)
                    tabList.add(toIndex, tab)

                    // Update positions for all affected tabs
                    tabList.forEachIndexed { index, tabEntity ->
                        repository.updateTabPosition(tabEntity.id, index)
                    }
                    Log.d("TabViewModel", "Moved tab from $fromIndex to $toIndex")
                }
            } catch (e: Exception) {
                Log.e("TabViewModel", "Failed to move tab: ${e.message}")
            }
        }
    }

    suspend fun getTabById(tabId: Long): TabEntity? {
        return try {
            repository.getTabById(tabId)
        } catch (e: Exception) {
            Log.e("TabViewModel", "Failed to get tab by ID: ${e.message}")
            null
        }
    }

    fun clearError() {
        _error.value = null
    }
}