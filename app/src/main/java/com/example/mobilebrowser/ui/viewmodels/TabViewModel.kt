package com.example.mobilebrowser.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.entity.TabEntity
import com.example.mobilebrowser.data.repository.TabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel responsible for managing tab-related operations and state.
 */
@HiltViewModel
class TabViewModel @Inject constructor(
    private val repository: TabRepository
) : ViewModel() {
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

    // State for tab selection mode
    private val _isSelectionModeActive = MutableStateFlow(false)
    val isSelectionModeActive = _isSelectionModeActive.asStateFlow()

    // Selected tabs in selection mode
    private val _selectedTabs = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTabs = _selectedTabs.asStateFlow()

    /**
     * Creates a new tab with the given URL and title
     */
    suspend fun createTab(url: String = "about:blank", title: String = "New Tab"): Long {
        return repository.createTab(
            url = url,
            title = title,
            position = tabCount.value
        )
    }

    /**
     * Updates an existing tab's content
     */
    fun updateTab(tab: TabEntity) {
        viewModelScope.launch {
            repository.updateTab(tab)
        }
    }

    /**
     * Switches to a specific tab
     */
    fun switchToTab(tabId: Long) {
        viewModelScope.launch {
            repository.switchToTab(tabId)
        }
    }

    /**
     * Closes a specific tab
     */
    fun closeTab(tab: TabEntity) {
        viewModelScope.launch {
            repository.deleteTab(tab)
        }
    }

    /**
     * Closes all tabs
     */
    fun closeAllTabs() {
        viewModelScope.launch {
            repository.deleteAllTabs()
        }
    }

    /**
     * Toggles selection mode
     */
    fun toggleSelectionMode() {
        _isSelectionModeActive.value = !_isSelectionModeActive.value
        if (!_isSelectionModeActive.value) {
            clearSelection()
        }
    }

    /**
     * Toggles selection of a specific tab
     */
    fun toggleTabSelection(tabId: Long) {
        val currentSelection = _selectedTabs.value.toMutableSet()
        if (currentSelection.contains(tabId)) {
            currentSelection.remove(tabId)
        } else {
            currentSelection.add(tabId)
        }
        _selectedTabs.value = currentSelection
    }

    /**
     * Clears all selected tabs
     */
    fun clearSelection() {
        _selectedTabs.value = emptySet()
    }

    /**
     * Closes selected tabs
     */
    fun closeSelectedTabs() {
        viewModelScope.launch {
            _selectedTabs.value.forEach { tabId ->
                repository.getTabById(tabId)?.let { tab ->
                    repository.deleteTab(tab)
                }
            }
            clearSelection()
            toggleSelectionMode()
        }
    }

    /**
     * Updates the URL and title of the active tab
     */
    fun updateActiveTabContent(url: String, title: String) {
        viewModelScope.launch {
            activeTab.value?.let { tab ->
                repository.updateTab(
                    tab.copy(
                        url = url,
                        title = title,
                        lastVisited = Date()
                    )
                )
            }
        }
    }
}