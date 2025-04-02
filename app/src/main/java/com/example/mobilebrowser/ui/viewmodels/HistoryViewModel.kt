package com.example.mobilebrowser.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilebrowser.data.entity.HistoryEntity
import com.example.mobilebrowser.data.repository.HistoryRepository
import com.example.mobilebrowser.sync.SyncStatusState
import com.example.mobilebrowser.sync.UserSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: HistoryRepository,
    private val userSyncManager: UserSyncManager
) : ViewModel() {
    // Expose local history data to the UI.
    val allHistory = repository.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Expose the sync status so the UI can show sync progress or errors.
    private val _syncStatus = MutableStateFlow<SyncStatusState>(SyncStatusState.Idle)
    val syncStatus: StateFlow<SyncStatusState> = _syncStatus.asStateFlow()

    /**
     * Allows the UI to trigger a manual sync operation.
     * This pushes any local pending changes (PENDING_UPLOAD) to the backend.
     *
     * @param authToken The authentication token for API calls.
     * @param deviceId The device identification header value.
     * @param userId The current user's identifier.
     */
    fun triggerManualSync(authToken: String, deviceId: String, userId: String) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatusState.Syncing
            try {
                userSyncManager.pushLocalChanges(authToken, deviceId, userId)
                _syncStatus.value = SyncStatusState.Synced
            } catch (e: Exception) {
                _syncStatus.value = SyncStatusState.Error(e.message ?: "Unknown error during sync")
            }
        }
    }

    // Search query state.
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Error state.
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Today's start time.
    private val todayStart: Date = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    // Current time.
    private val now = Date()

    // Last week's start time (excluding today).
    private val lastWeekStart: Date = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -7)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    // All history entries for combined display and debugging.
    private val allHistoryEntries = repository.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Today's history entries.
    val todayHistory = allHistoryEntries
        .map { entries ->
            entries.filter { entry ->
                entry.lastVisited.time >= todayStart.time
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Last week's history (excluding today).
    val lastWeekHistory = allHistoryEntries
        .map { entries ->
            entries.filter { entry ->
                entry.lastVisited.time < todayStart.time && entry.lastVisited.time >= lastWeekStart.time
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Search results.
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults = _searchQuery
        .debounce(300L) // Debounce search input.
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                repository.searchHistory(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Recent history entries.
    val recentHistory = repository.getRecentHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Update search query.
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Adds a history entry.
     *
     * If the user is signed in, pass the valid userId;
     * if not, the default empty string ("") is used to mark this entry as local-only.
     */
    fun addHistoryEntry(url: String, title: String, favicon: String? = null, userId: String = "") {
        viewModelScope.launch {
            try {
                repository.addHistoryEntry(url, title, favicon, userId)
            } catch (e: Exception) {
                _error.value = "Failed to add history entry: ${e.message}"
            }
        }
    }

    // Delete a specific history entry.
    fun deleteHistoryEntry(history: HistoryEntity) {
        viewModelScope.launch {
            try {
                repository.deleteHistoryEntry(history)
            } catch (e: Exception) {
                _error.value = "Failed to delete history entry: ${e.message}"
            }
        }
    }

    // Delete history by a specified time range.
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

    // Clear any error messages.
    fun clearError() {
        _error.value = null
    }

    // Expose all history entries for debugging purposes.
    fun getAllHistoryForDebug(): Flow<List<HistoryEntity>> {
        return repository.getAllHistory()
    }
}

// Enum for history deletion time ranges.
enum class HistoryTimeRange {
    LAST_HOUR,
    TODAY,
    YESTERDAY,
    ALL
}
