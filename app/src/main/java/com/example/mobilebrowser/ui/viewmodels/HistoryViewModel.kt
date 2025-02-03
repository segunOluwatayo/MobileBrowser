package com.example.mobilebrowser.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.entity.HistoryEntity
import com.example.mobilebrowser.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository
) : ViewModel() {
    // Search query state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // History entries filtered by search query
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val historyEntries = _searchQuery
        .debounce(300L) // Debounce search input
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllHistory()
            } else {
                repository.searchHistory(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Update search query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Add history entry
    fun addHistoryEntry(url: String, title: String, favicon: String? = null) {
        viewModelScope.launch {
            try {
                repository.addHistoryEntry(url, title, favicon)
            } catch (e: Exception) {
                _error.value = "Failed to add history entry: ${e.message}"
            }
        }
    }

    // Delete specific history entry
    fun deleteHistoryEntry(history: HistoryEntity) {
        viewModelScope.launch {
            try {
                repository.deleteHistoryEntry(history)
            } catch (e: Exception) {
                _error.value = "Failed to delete history entry: ${e.message}"
            }
        }
    }

    // Delete history by time range
    fun deleteHistoryByTimeRange(timeRange: HistoryTimeRange) {
        viewModelScope.launch {
            try {
                when (timeRange) {
                    HistoryTimeRange.LAST_HOUR -> repository.deleteLastHourHistory()
                    HistoryTimeRange.TODAY -> repository.deleteTodayHistory()
                    HistoryTimeRange.YESTERDAY -> repository.deleteYesterdayHistory()
                    HistoryTimeRange.ALL -> repository.deleteAllHistory()
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete history: ${e.message}"
            }
        }
    }

    // Clear error
    fun clearError() {
        _error.value = null
    }
}

// Enum for history deletion time ranges
enum class HistoryTimeRange {
    LAST_HOUR,
    TODAY,
    YESTERDAY,
    ALL
}